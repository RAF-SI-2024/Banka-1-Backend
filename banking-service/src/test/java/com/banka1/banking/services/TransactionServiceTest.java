package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private BankAccountUtils bankAccountUtils;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private Transfer internalTransfer;
    private Transfer externalTransfer;
    private Currency currency;

    @BeforeEach
    void setUp() {
        // Setup test accounts
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(1000.0);
        fromAccount.setCurrencyType(CurrencyType.USD);


        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(500.0);
        toAccount.setCurrencyType(CurrencyType.USD);

        // Setup currency
        currency = new Currency();
        currency.setCode(CurrencyType.USD);

        // Setup internal transfer
        internalTransfer = new Transfer();
        internalTransfer.setId(1L);
        internalTransfer.setFromAccountId(fromAccount);
        internalTransfer.setToAccountId(toAccount);
        internalTransfer.setAmount(100.0);
        internalTransfer.setStatus(TransferStatus.PENDING);
        internalTransfer.setType(TransferType.INTERNAL);
        internalTransfer.setFromCurrency(currency);
        internalTransfer.setToCurrency(currency);

        // Setup external transfer
        externalTransfer = new Transfer();
        externalTransfer.setId(2L);
        externalTransfer.setFromAccountId(fromAccount);
        externalTransfer.setToAccountId(toAccount);
        externalTransfer.setAmount(100.0);
        externalTransfer.setStatus(TransferStatus.PENDING);
        externalTransfer.setType(TransferType.EXTERNAL);
        externalTransfer.setFromCurrency(currency);
        externalTransfer.setToCurrency(currency);
    }

    @Test
    void testGetTransactionsByUserId() {
        Long userId = 1L;
        List<Account> accounts = Arrays.asList(fromAccount);
        List<Transaction> expectedTransactions = Arrays.asList(new Transaction(), new Transaction());

        when(bankAccountUtils.getBankAccountForCurrency(any())).thenReturn(new Account());

        when(accountRepository.findByOwnerID(userId)).thenReturn(accounts);
        when(transactionRepository.findByFromAccountIdInOrToAccountIdIn(accounts, accounts)).thenReturn(expectedTransactions);

        List<Transaction> actualTransactions = transactionService.getTransactionsByUserId(userId);

        assertEquals(expectedTransactions, actualTransactions);
    }
}