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
package com.avanza.astrix.test.util;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoCloseableExtension implements AfterEachCallback {

    private final Queue<AutoCloseable> autoClosables = new ConcurrentLinkedQueue<>();

    @Override
    public void afterEach(ExtensionContext context) {
		autoClosables.forEach(AstrixTestUtil::closeQuiet);
    }

    public <T extends AutoCloseable> T add(T autoClosable) {
        autoClosables.add(autoClosable);
        return autoClosable;
    }

}
