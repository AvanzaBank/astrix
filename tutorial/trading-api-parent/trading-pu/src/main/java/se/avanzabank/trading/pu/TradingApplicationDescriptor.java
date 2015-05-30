package se.avanzabank.trading.pu;

import se.avanzabank.trading.api.runtime.TradingServiceApiProvider;

import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApplication;

@AstrixApplication(
	exportsRemoteServicesFor = TradingServiceApiProvider.class,
	defaultServiceComponent = AstrixServiceComponentNames.GS_REMOTING
)
public class TradingApplicationDescriptor {
}
