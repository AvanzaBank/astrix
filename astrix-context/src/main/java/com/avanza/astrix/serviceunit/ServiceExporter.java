package com.avanza.astrix.serviceunit;

public interface ServiceExporter {

	void addServiceProvider(Object bean);

	void exportService(ExportedServiceBeanDefinition definition);

	void exportProvidedServices();

	void setServiceDescriptor(AstrixApplicationDescriptor applicationDescriptor);

	void startPublishServices();

}
