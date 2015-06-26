package com.avanza.astrix.gs;

import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.gs.ClusteredProxyCacheImpl.GigaSpaceInstance;

public interface ClusteredProxyCache {

	GigaSpaceInstance getProxy(ServiceProperties serviceProperties);

}
