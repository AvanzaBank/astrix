package se.avanzabank.trading.pu;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.provider.core.AstrixServiceExport;

import se.avanzabank.trading.api.Account;
import se.avanzabank.trading.api.AccountId;
import se.avanzabank.trading.api.AccountService;

@AstrixServiceExport(AccountService.class)
public class AccountServiceImpl implements AccountService {
	
	private final GigaSpace gigaSpace;

	@Autowired
	public AccountServiceImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public Account getAccount(AccountId id) {
		SpaceAccount spaceAccount = gigaSpace.readById(SpaceAccount.class, id);
		if (spaceAccount == null) {
			return null;
		}
		return toAccount(spaceAccount);
	}

	@Override
	public void registerAccount(Account account) {
		gigaSpace.write(toSpaceAccount(account));
	}

	private SpaceAccount toSpaceAccount(Account account) {
		SpaceAccount result = new SpaceAccount();
		result.setBalance(account.getBalance());
		result.setId(account.getId());
		return result;
	}

	private Account toAccount(SpaceAccount spaceAccount) {
		return new Account(spaceAccount.getId(), spaceAccount.getBalance());
	}


}
