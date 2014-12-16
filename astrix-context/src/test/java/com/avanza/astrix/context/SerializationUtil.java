/*
 * Copyright 2014-2015 Avanza Bank AB
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

public class SerializationUtil {
	
	public static Object readObject(byte[] data) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
			try (ObjectInput in = new ObjectInputStream(bis)) {
				return in.readObject();
			}
		}
	}
	
	public static byte[] writeObject(Object object) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (ObjectOutput out = new ObjectOutputStream(bos)) {
				out.writeObject(object);
				return bos.toByteArray();
			}
		}
	}
	

}
