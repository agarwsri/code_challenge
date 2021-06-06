package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();

	private static ReentrantLock lock = new ReentrantLock(true);

	@Autowired
	private NotificationService notificationService;

	@Override
	public void createAccount(Account account) throws DuplicateAccountIdException {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException(
					"Account id " + account.getAccountId() + " already exists!");
		}
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

	@Override
	public void transferMoney(MoneyTransferRequest request) {
		Account fromAccount = accounts.get(request.getFromAccountId());
		Account beneficiaryAccount = null;
		if(null != fromAccount) {
			beneficiaryAccount = accounts.get(request.getToAccountId());
			if(null != beneficiaryAccount) {
				lock.lock();
				BigDecimal remainingBalance = fromAccount.getBalance().
						subtract(request.getAmountToTransfer());
				if(remainingBalance.compareTo(BigDecimal.ZERO) >= 0) {
					fromAccount.setBalance(remainingBalance);
					beneficiaryAccount.setBalance(beneficiaryAccount.getBalance().add(request.getAmountToTransfer()));
					try {
						notificationService.notifyAboutTransfer(fromAccount, "Transferred "+request.getAmountToTransfer()
						+" to beneficiary account id "+beneficiaryAccount.getAccountId());
						notificationService.notifyAboutTransfer(beneficiaryAccount, "Received "+request.getAmountToTransfer()
						+" from account id "+fromAccount.getAccountId());
					}
					catch(Exception e) {
						log.warn("Exception occured when executing notification service "+e);
					}
					lock.unlock();
				}else {
					lock.unlock();
					throw new InsufficientBalanceException("Insufficient balance in Account after transfer for id "
							+request.getFromAccountId()+" value after transfer is "+remainingBalance);
				}
			}else {
				throw new AccountNotFoundException("Beneficiary Account is not present "
						+request.getToAccountId()+" unable to do money transfer !");
			}
		}else {
			throw new AccountNotFoundException("Source Account is not present "
					+request.getFromAccountId()+" unable to do money transfer !");
		}
	}


}
