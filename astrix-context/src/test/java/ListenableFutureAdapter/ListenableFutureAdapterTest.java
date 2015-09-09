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
package ListenableFutureAdapter;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.avanza.astrix.beans.core.ListenableFutureAdapter;

import rx.Observable;

public class ListenableFutureAdapterTest {
	
	@Test
	public void ifResultIsAlreadyDoneThenListenerShouldBeNotifiedSynchronously() throws Exception {
		ListenableFutureAdapter<String> listenable = new ListenableFutureAdapter<>(Observable.just("2"));
		assertEquals("2", listenable.get());
		AtomicReference<String> result = new AtomicReference<String>("");
		listenable.setFutureListener((r) -> result.set(r.getResult()));
		assertEquals("2", result.get());
	}

}
