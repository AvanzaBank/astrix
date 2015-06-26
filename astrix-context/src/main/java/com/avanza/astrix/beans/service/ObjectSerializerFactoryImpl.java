package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.service.AstrixVersioningPlugin;
import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.beans.service.ObjectSerializerFactory;
import com.avanza.astrix.core.AstrixObjectSerializer;

class ObjectSerializerFactoryImpl implements ObjectSerializerFactory {

	private AstrixVersioningPlugin versioningPlugin;

	public ObjectSerializerFactoryImpl(AstrixVersioningPlugin versioningPlugin) {
		this.versioningPlugin = versioningPlugin;
	}

	@Override
	public AstrixObjectSerializer create(ObjectSerializerDefinition serializerDefinition) {
		if (serializerDefinition.isVersioned()) {
			return versioningPlugin.create(serializerDefinition); 
		}
		return new AstrixObjectSerializer.NoVersioningSupport();
	}

}
