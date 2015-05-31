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
package se.avanzabank.trading.api;

import java.io.Serializable;
import java.util.Objects;

public final class AccountId implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private final String id;

	private AccountId(String id) {
		this.id = Objects.requireNonNull(id);
	}
	
	public static AccountId valueOf(String id) {
		return new AccountId(id);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccountId other = (AccountId) obj;
		return Objects.equals(this.id, other.id);
	}

	@Override
	public String toString() {
		return id;
	}

}
