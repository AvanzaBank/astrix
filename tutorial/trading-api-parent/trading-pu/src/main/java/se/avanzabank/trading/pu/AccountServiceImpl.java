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
