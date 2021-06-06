package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void doMoneyTransferWithInsuffBalAfterTransfer() throws Exception {
		String uniqueId = "Id-1290";
		Account fromAccount = new Account(uniqueId);
		this.accountsService.createAccount(fromAccount);
		fromAccount.setBalance(new BigDecimal(3000));

		String uniqueIdTo = "Id-1291";
		Account toAccount = new Account(uniqueIdTo);
		this.accountsService.createAccount(toAccount);
		toAccount.setBalance(new BigDecimal(3000));

		try {
			this.accountsService.transferMoney(new MoneyTransferRequest(fromAccount.getAccountId(),
					toAccount.getAccountId(), new BigDecimal(3100)));
		} catch (InsufficientBalanceException ex) {
			assertThat(ex.getMessage()).isEqualTo("Insufficient balance in Account after transfer for id "
					+fromAccount.getAccountId()+" value after transfer is "+fromAccount.getBalance().subtract(new BigDecimal(3100)));
		}

	}

	@Test
	public void transferMoneyWithNoFromAccountFound() throws Exception {
		String uniqueId = "Id-8888";
		Account fromAccount = new Account(uniqueId);
		this.accountsService.createAccount(fromAccount);
		fromAccount.setBalance(new BigDecimal(3000));

		String uniqueIdTo = "Id-9999";
		Account toAccount = new Account(uniqueIdTo);
		this.accountsService.createAccount(toAccount);
		toAccount.setBalance(new BigDecimal(3000));

		try {
			this.accountsService.transferMoney(new MoneyTransferRequest("Id-123232",
					toAccount.getAccountId(), new BigDecimal(400)));
		} catch (AccountNotFoundException ex) {
			assertThat(ex.getMessage()).isEqualTo("Source Account is not present Id-123232 unable to do money transfer !");
		}

	}

	@Test
	public void transferMoneyWithNoBeneficiaryAccountFound() throws Exception {
		String uniqueId = "Id-6567";
		Account fromAccount = new Account(uniqueId);
		this.accountsService.createAccount(fromAccount);
		fromAccount.setBalance(new BigDecimal(3000));

		String uniqueIdTo = "Id-2342";
		Account toAccount = new Account(uniqueIdTo);
		this.accountsService.createAccount(toAccount);
		toAccount.setBalance(new BigDecimal(3000));

		try {
			this.accountsService.transferMoney(new MoneyTransferRequest(fromAccount.getAccountId(),
					"Id-1234", new BigDecimal(400)));
		} catch (AccountNotFoundException ex) {
			assertThat(ex.getMessage()).isEqualTo("Beneficiary Account is not present Id-1234 unable to do money transfer !");
		}
	}

	@Test
	public void doMoneyTransferConcurrently() throws Exception {
		String uniqueId = "Id-111";
		Account fromAccount = new Account(uniqueId,new BigDecimal(10000));
		this.accountsService.createAccount(fromAccount);

		String uniqueIdTo = "Id-222";
		Account toAccount = new Account(uniqueIdTo,new BigDecimal(9000));
		this.accountsService.createAccount(toAccount);

		ExecutorService service = Executors.newFixedThreadPool(10);
		MoneyTransferRequest transferRequest = new MoneyTransferRequest(fromAccount.getAccountId(),
				toAccount.getAccountId(),new BigDecimal(10));

		for(int i=0;i<10;i++) {
			service.execute(()->this.accountsService.transferMoney(transferRequest));
		}
		service.awaitTermination(2, TimeUnit.MINUTES);
		
		
		assertThat(this.accountsService.getAccount(fromAccount.getAccountId()).getBalance()).isEqualTo(new BigDecimal(9900));
		assertThat(this.accountsService.getAccount(toAccount.getAccountId()).getBalance()).isEqualTo(new BigDecimal(9100));
	}
	
	@Test
	public void doMoneyTransfer() throws Exception {
		String uniqueId = "Id-198";
		Account fromAccount = new Account(uniqueId,new BigDecimal(10000));
		this.accountsService.createAccount(fromAccount);

		String uniqueIdTo = "Id-298";
		Account toAccount = new Account(uniqueIdTo,new BigDecimal(29000));
		this.accountsService.createAccount(toAccount);


		MoneyTransferRequest transferRequest = new MoneyTransferRequest(fromAccount.getAccountId(),
				toAccount.getAccountId(),new BigDecimal(3000));

		this.accountsService.transferMoney(transferRequest);	
		
		assertThat(this.accountsService.getAccount(fromAccount.getAccountId()).getBalance()).isEqualTo(new BigDecimal(7000));
		assertThat(this.accountsService.getAccount(toAccount.getAccountId()).getBalance()).isEqualTo(new BigDecimal(32000));
	}

}
