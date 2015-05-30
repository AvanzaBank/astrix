package se.avanzabank.trading.api.runtime;

import se.avanzabank.trading.api.AccountService;

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;

@AstrixApiProvider
public interface TradingServiceApiProvider {
	@Service
	AccountService accountService();
}
