package se.avanzabank.trading.api;

import com.avanza.astrix.core.AstrixRouting;

public interface AccountService {
	
	Account getAccount(@AstrixRouting AccountId id);
	
	void registerAccount(@AstrixRouting("getId") Account account);

}
