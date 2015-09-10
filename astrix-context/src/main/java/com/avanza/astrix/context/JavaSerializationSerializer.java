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
package com.avanza.astrix.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;

import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
/**
 * {@link AstrixObjectSerializer} using java serialization mechanism. Does not use version number. <p>
 * 
 * Mainly used to support testing. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class JavaSerializationSerializer implements AstrixObjectSerializer {
	
	public JavaSerializationSerializer(int version) {
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(Object element, Type type, int version) {
		try {
			byte[] data = (byte[]) element;
			return (T) readObject(data);
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException("Deserialization failed", e);
		}
	}
	
	@Override
	public Object serialize(Object element, int version) {
		try {
			return writeObject(element);
		} catch (IOException e) {
			throw new RuntimeException("Serialization failed", e);
		}
	}
	
	@Override
	public int version() {
		return 1;
	}
	
	private static Object readObject(byte[] data) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
			try (ObjectInput in = new ObjectInputStream(bis)) {
				return in.readObject();
			}
		}
	}
	
	private static byte[] writeObject(Object object) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (ObjectOutput out = new ObjectOutputStream(bos)) {
				out.writeObject(object);
				return bos.toByteArray();
			}
		}
	}
}