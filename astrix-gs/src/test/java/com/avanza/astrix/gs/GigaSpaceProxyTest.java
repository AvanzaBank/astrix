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
package com.avanza.astrix.gs;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.gigaspaces.internal.client.cache.SpaceCacheException;
import org.junit.jupiter.api.Test;
import org.openspaces.core.GigaSpace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GigaSpaceProxyTest {

    @Test
    void proxiesInvocations() {
        GigaSpace gigaSpace = mock(GigaSpace.class);
        GigaSpace proxied = GigaSpaceProxy.create(gigaSpace);

        when(gigaSpace.count(null)).thenReturn(21);

        assertEquals(21, proxied.count(null));
    }

    @Test
    void wrapsSpaceCacheExceptionsInServiceUnavailableException() {
        GigaSpace gigaSpace = mock(GigaSpace.class);

        when(gigaSpace.count(null)).thenThrow(new SpaceCacheException(""));
        GigaSpace proxied = GigaSpaceProxy.create(gigaSpace);

        assertThrows(ServiceUnavailableException.class, () -> proxied.count(null), "Expected ServiceUnavailableException or subclass to be thrown");
    }

}
