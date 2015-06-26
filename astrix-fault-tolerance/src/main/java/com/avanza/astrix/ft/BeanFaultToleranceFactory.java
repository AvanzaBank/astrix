package com.avanza.astrix.ft;

import com.avanza.astrix.beans.publish.PublishedAstrixBean;

public interface BeanFaultToleranceFactory {

	BeanFaultTolerance create(PublishedAstrixBean<?> serviceDefinition);

}
