/*
 * Copyright 2014 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.remoting.server;

import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.API_VERSION;
import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.SERVICE_API;
import static com.avanza.astrix.remoting.client.AstrixServiceInvocationRequestHeaders.SERVICE_METHOD_SIGNATURE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.InvocationExecutionWatcher;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.context.mbeans.MBeanExporter;
import com.avanza.astrix.context.metrics.Metrics;
import com.avanza.astrix.context.metrics.Timer;
import com.avanza.astrix.core.ServiceInvocationException;
import com.avanza.astrix.core.function.Command;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.modules.AstrixInject;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponse;
import com.avanza.astrix.remoting.client.AstrixServiceInvocationResponseHeaders;
import com.avanza.astrix.remoting.client.MissingServiceMethodException;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import java.util.Objects;
/**
 * Server side component used to invoke exported services. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class AstrixServiceActivatorImpl implements AstrixServiceActivator {
	
	private static final Logger logger = LoggerFactory.getLogger(AstrixServiceActivatorImpl.class);
	private final ConcurrentMap<String, PublishedService<?>> serviceByType = new ConcurrentHashMap<>();
	private final Metrics metrics;
	private final MBeanExporter mbeanExporter;
	private final ServiceInvocationMonitor allServicesAggregated;
	private final DynamicBooleanProperty exportedServiceMetricsEnabled;
	private final AstrixTraceProvider astrixTraceProvider;

	@AstrixInject
	public AstrixServiceActivatorImpl(
			AstrixConfig astrixConfig,
			Metrics metrics,
			MBeanExporter mbeanExporter,
			AstrixTraceProvider astrixTraceProvider
	) {
		this(astrixConfig.get(AstrixSettings.EXPORTED_SERVICE_METRICS_ENABLED), metrics, mbeanExporter, astrixTraceProvider);
	}
	
	// For testnig
	AstrixServiceActivatorImpl(
			DynamicBooleanProperty exportedServiceMetricsEnabled,
			Metrics metrics,
			MBeanExporter mbeanExporter,
			AstrixTraceProvider astrixTraceProvider
	) {
		this.exportedServiceMetricsEnabled = exportedServiceMetricsEnabled;
		this.metrics = metrics;
		this.mbeanExporter = mbeanExporter;
		// Monitor for aggregated stats for all exported services
		this.allServicesAggregated = new ServiceInvocationMonitor(metrics.createTimer());
		mbeanExporter.registerMBean(this.allServicesAggregated, "ExportedServices", "AllServicesAggregated");
		this.astrixTraceProvider = Objects.requireNonNull(astrixTraceProvider);
	}
	
	private static class ServiceInvocationMonitors {
		private final List<ServiceInvocationMonitor> monitor;
		private final DynamicBooleanProperty serviceMonitorEnabled;
		
		public ServiceInvocationMonitors(DynamicBooleanProperty serviceMonitorEnabled, ServiceInvocationMonitor... monitors) {
			this.serviceMonitorEnabled = serviceMonitorEnabled;
			this.monitor = Arrays.asList(monitors);
		}

		public Command<AstrixServiceInvocationResponse> monitorServiceInvocation(Command<AstrixServiceInvocationResponse> execution) {
			if (!serviceMonitorEnabled.get()) {
				return execution;
			}
			for (ServiceInvocationMonitor monitor : monitor) {
				execution = monitor.monitor(execution);
			}
			return execution;
		}
	}
	
	private static class PublishedServiceMethod<T> {
		private final ServiceInvocationMonitors serviceInvocationMonitors;
		private final List<InvocationExecutionWatcher> invocationExecutionWatchers;
		private final Method serviceMethod;
		private final AstrixObjectSerializer objectSerializer;
		private final T service;
		private final AstrixTraceProvider astrixTraceProvider;

		public PublishedServiceMethod(
				ServiceInvocationMonitors serviceInvocationMonitors,
				List<InvocationExecutionWatcher> invocationExecutionWatchers,
				Method method,
				AstrixObjectSerializer objectSerializer,
				T service,
				AstrixTraceProvider astrixTraceProvider
		) {
			this.serviceInvocationMonitors = serviceInvocationMonitors;
			this.invocationExecutionWatchers = invocationExecutionWatchers;
			this.serviceMethod = method;
			this.objectSerializer = objectSerializer;
			this.service = service;
			this.astrixTraceProvider = astrixTraceProvider;
		}
		
		private AstrixServiceInvocationResponse timeInvocation(AstrixServiceInvocationRequest request, int version) {
			return serviceInvocationMonitors.monitorServiceInvocation(() -> invoke(request, version)).call();
		}
		
		private AstrixServiceInvocationResponse invoke(AstrixServiceInvocationRequest request, int version) {
			Runnable afterInvocationWatchers = InvocationExecutionWatcher.apply(invocationExecutionWatchers, request.getHeaders());
			try {
				return invokeService(request, version);
			} catch (Exception e) {
				Throwable exceptionThrownByService = resolveException(e);
				AstrixServiceInvocationResponse invocationResponse = new AstrixServiceInvocationResponse();
				invocationResponse.setExceptionMsg(exceptionThrownByService.getMessage());
				invocationResponse.setCorrelationId(getCorrelationId(astrixTraceProvider, request));
				if (exceptionThrownByService instanceof ServiceInvocationException) {
					invocationResponse.setException(this.objectSerializer.serialize(exceptionThrownByService, version));
				} else {
					invocationResponse.setThrownExceptionType(exceptionThrownByService.getClass().getName());
				}
				logger.info(String.format("Service invocation ended with exception. request=%s correlationId=%s", request, invocationResponse.getCorrelationId()), exceptionThrownByService);
				return invocationResponse;
			} finally {
				afterInvocationWatchers.run();
			}
		}

		private AstrixServiceInvocationResponse invokeService(AstrixServiceInvocationRequest request, int version) throws IllegalAccessException,
				InvocationTargetException {
			Object[] arguments = unmarshal(request.getArguments(), serviceMethod.getGenericParameterTypes(), version);
			Object result = serviceMethod.invoke(service, arguments);
			AstrixServiceInvocationResponse invocationResponse = new AstrixServiceInvocationResponse();
			if (serviceMethod.getReturnType().equals(Void.TYPE)) {
				return invocationResponse;
			}
			if (serviceMethod.getReturnType().equals(Optional.class)) {
				if (result == null) {
					invocationResponse.setHeader(AstrixServiceInvocationResponseHeaders.OPTIONAL_RETURN_VALUE_IS_NULL, "true");
				} else {
					invocationResponse.setResponseBody(objectSerializer.serialize(Optional.class.cast(result).orElse(null), version));
				}
			} else {
				invocationResponse.setResponseBody(objectSerializer.serialize(result, version));
			}
			return invocationResponse;
		}

		private Object[] unmarshal(Object[] elements, Type[] types, int version) {
			Object[] result = new Object[elements.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = objectSerializer.deserialize(elements[i], types[i], version);
			}
			return result;
		}

	}

	class PublishedService<T> {

		private final Map<String, PublishedServiceMethod<T>> methodBySignature = new HashMap<>();
		/*
		 * Overloaded service methods share the same Metrics
		 */
		private final Map<String, ServiceInvocationMonitors> serviceInvocationMonitorsByMethodName = new HashMap<>();
		private final AstrixObjectSerializer objectSerializer;
		private Class<?> providedApi;
		private ServiceInvocationMonitor serviceMonitor;

		public PublishedService(T service, AstrixObjectSerializer serializer, Class<?> providedApi) {
			this.objectSerializer = serializer;
			this.providedApi = providedApi;
			// Monitor for service-level metrics (aggregated stats for all methods)
			this.serviceMonitor = new ServiceInvocationMonitor(metrics.createTimer());
			mbeanExporter.registerMBean(this.serviceMonitor, "ExportedServices", providedApi.getName());
			for (Method m : providedApi.getMethods()) {
				ServiceInvocationMonitors serviceInvocationMonitors = serviceInvocationMonitorsByMethodName.computeIfAbsent(m.getName(), this::createServiceInvocationMonitors);
				List<InvocationExecutionWatcher> invocationExecutionWatchers = astrixTraceProvider.getServerCallExecutionWatchers(providedApi.getName(), m.getName());
				methodBySignature.put(
						ReflectionUtil.methodSignatureWithoutReturnType(m),
						new PublishedServiceMethod<>(
								serviceInvocationMonitors,
								invocationExecutionWatchers,
								m,
								objectSerializer,
								service,
								astrixTraceProvider
						)
				);
			}
		}

		private ServiceInvocationMonitors createServiceInvocationMonitors(String methodName) {
			Timer methodTimer = metrics.createTimer();
			// Monitor for method level metrics
			ServiceInvocationMonitor methodMonitor = new ServiceInvocationMonitor(methodTimer);
			mbeanExporter.registerMBean(methodMonitor, "ExportedServices", providedApi.getName() + "#" + methodName);
			ServiceInvocationMonitors result = new ServiceInvocationMonitors(exportedServiceMetricsEnabled, methodMonitor, serviceMonitor, allServicesAggregated);
			return result;
		}
		
		private AstrixServiceInvocationResponse invoke(AstrixServiceInvocationRequest request, int version, String serviceApi) {
			String serviceMethodSignature = request.getHeader(SERVICE_METHOD_SIGNATURE);
			PublishedServiceMethod<T> serviceMethod = methodBySignature.get(serviceMethodSignature);
			if (serviceMethod == null) {
				throw new MissingServiceMethodException(String.format("Missing service method: service=%s method=%s", serviceApi, serviceMethodSignature));
			}
			return serviceMethod.timeInvocation(request, version);
		}
		
	}
	
	@Override
	public void register(Object provider, AstrixObjectSerializer objectSerializer, Class<?> publishedApi) {
		if (!publishedApi.isAssignableFrom(provider.getClass())) {
			throw new IllegalArgumentException("Provider: " + provider.getClass() + " does not implement: " + publishedApi);
		}
		PublishedService<?> publishedService = new PublishedService<>(provider, objectSerializer, publishedApi);
		this.serviceByType.put(publishedApi.getName(), publishedService);
	}
	
	/**
	 * @param request
	 * @return
	 */
	@Override
	public AstrixServiceInvocationResponse invokeService(final AstrixServiceInvocationRequest request) {
		final int version = Integer.parseInt(request.getHeader(API_VERSION));
		final String serviceApi = request.getHeader(SERVICE_API);
		final PublishedService<?> publishedService = this.serviceByType.get(serviceApi);
		if (publishedService == null) {
			/*
			 * Service not available. This might happen in rare conditions when a processing unit
			 * is restarted and old clients connects to the space before the framework is fully initialized. 
			 */
			AstrixServiceInvocationResponse invocationResponse = new AstrixServiceInvocationResponse();
			invocationResponse.setServiceUnavailable(true);
			invocationResponse.setExceptionMsg("Service not available in service activator: " + serviceApi);
			invocationResponse.setCorrelationId(getCorrelationId(astrixTraceProvider, request));
			logger.info(String.format("Service not available. request=%s correlationId=%s", request, invocationResponse.getCorrelationId()));
			return invocationResponse;
		}
		return publishedService.invoke(request, version, serviceApi);
	}

	private static Throwable resolveException(Exception e) {
		if (e instanceof InvocationTargetException) {
			// Invoked service threw an exception
			return InvocationTargetException.class.cast(e).getTargetException();
		}
		return e;
	}

	private static String getCorrelationId(
			AstrixTraceProvider astrixTraceProvider,
			AstrixServiceInvocationRequest request
	) {
		return astrixTraceProvider.getCorrelationId(request.getHeaders());
	}
}
