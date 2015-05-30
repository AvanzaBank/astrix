package se.avanzabank.trading.pu;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Test;

import se.avanzabank.trading.api.Account;
import se.avanzabank.trading.api.AccountId;
import se.avanzabank.trading.api.AccountService;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;

public class TradingPuTest {
	
	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@ClassRule
	public static final RunningPu tradingPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/pu.xml")
														   .startAsync(false)
														   .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
														   .configure();
	
	@Test
	public void accountServiceConsumtionExample() throws Exception {
		AstrixContext context = new AstrixConfigurer().set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri())
													  .configure();
		AccountService accountService = context.getBean(AccountService.class);
	
		assertNull(accountService.getAccount(AccountId.valueOf("21")));
		
		accountService.registerAccount(new Account(AccountId.valueOf("21"), 1000D));
		assertNotNull(accountService.getAccount(AccountId.valueOf("21")));
	}

}
