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
