package se.avanzabank.trading.api;

import java.io.Serializable;

public final class Account implements Serializable {

	private static final long serialVersionUID = 1L;

	private AccountId id;
	private double balance;

	public Account(AccountId id, double balance) {
		this.id = id;
		this.balance = balance;
	}

	public AccountId getId() {
		return id;
	}

	public double getBalance() {
		return balance;
	}

}
