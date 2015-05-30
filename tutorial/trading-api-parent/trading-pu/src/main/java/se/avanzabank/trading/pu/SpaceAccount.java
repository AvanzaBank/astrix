package se.avanzabank.trading.pu;

import com.gigaspaces.annotation.pojo.SpaceId;

import se.avanzabank.trading.api.AccountId;

public class SpaceAccount {
	
	private AccountId id;
	private double balance;

	@SpaceId(autoGenerate = false)
	public AccountId getId() {
		return id;
	}
	
	public void setId(AccountId id) {
		this.id = id;
	}
	
	public double getBalance() {
		return balance;
	}
	
	public void setBalance(double balance) {
		this.balance = balance;
	}

}
