package com.banka1.banking.service;

import com.banka1.banking.models.*;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.InstallmentsRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.services.LoanService;
import com.banka1.banking.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanPaymentSchedulerTest {

    @Mock
    private InstallmentsRepository installmentsRepository;
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private LoanService loanService;

    private Installment installment1;
    private Installment installment2;
    private Account userAccount;
    private Account bankAccount;
    private Loan loan;

    @BeforeEach
    void setUp() {
        userAccount = new Account();
        userAccount.setBalance(5000.0);
        userAccount.setCurrencyType(CurrencyType.AUD);

        bankAccount = new Account();
        bankAccount.setBalance(10000.0);

        loan = new Loan();
        loan.setLoanAmount(1000.0);
        loan.setNumberOfInstallments(12);
        loan.setAccount(userAccount);

        installment1 = new Installment();
        installment1.setLoan(loan);
        installment1.setInterestRate(5.0); // 5% interest
        installment1.setAttemptCount(0);
        installment1.setIsPaid(false);

        installment2 = new Installment();
        installment2.setLoan(loan);
        installment2.setInterestRate(5.0); // 5% interest
        installment2.setAttemptCount(0);
        installment2.setIsPaid(false);

    }

    @Test
    void testProcessLoanPaymentsSuccess() {
        when(installmentsRepository.getDueInstallments(anyLong())).thenReturn(Arrays.asList(installment1, installment2));

        when(transactionService.processInstallment(any(), any(), any())).thenReturn(true);

        loanService.processDueInstallments();

        assertTrue(installment1.getIsPaid());
        assertTrue(installment2.getIsPaid());
        assertNotNull(installment1.getActualDueDate());
        assertNotNull(installment2.getActualDueDate());

        verify(installmentsRepository, times(1)).save(installment1);
        verify(installmentsRepository, times(1)).save(installment2);
    }

    @Test
    void testProcessLoanPaymentsFailed() {
        when(installmentsRepository.getDueInstallments(anyLong())).thenReturn(Arrays.asList(installment1, installment2));

        when(transactionService.processInstallment(any(), any(), any())).thenReturn(false);

        loanService.processDueInstallments();

        assertFalse(installment1.getIsPaid());
        assertFalse(installment2.getIsPaid());
        assertNotNull(installment1.getRetryDate());
        assertNotNull(installment2.getRetryDate());

        verify(installmentsRepository, times(1)).save(installment1);
        verify(installmentsRepository, times(1)).save(installment2);
    }

    @Test
    void testProcessDueInstallments_NoInstallmentsFound() {
        when(installmentsRepository.getDueInstallments(anyLong())).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> {
            loanService.processDueInstallments();
        });

        verify(installmentsRepository, never()).save(any());
    }

}

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account userAccount;
    private Account bankAccount;
    private Loan loan;
    private Installment installment;

    @BeforeEach
    void setUp() {
        // Setup test data
        userAccount = new Account();
        userAccount.setBalance(5000.0);
        userAccount.setCurrencyType(CurrencyType.AUD);

        bankAccount = new Account();
        bankAccount.setBalance(10000.0);

        loan = new Loan();
        loan.setLoanAmount(1000.0);
        loan.setNumberOfInstallments(12);
        loan.setAccount(userAccount);

        installment = new Installment();
        installment.setLoan(loan);
        installment.setInterestRate(5.0);
    }

    @Test
    void testProcessInstallment_Success() {

        when(currencyRepository.getByCode(any())).thenReturn(new Currency());
        Double expectedInstallment = transactionService.calculateInstallment(1000.0, 5.0, 12);

        Boolean result = transactionService.processInstallment(userAccount, bankAccount, installment);

        assertTrue(result);
        assertEquals(5000.0 - expectedInstallment, userAccount.getBalance()); // Balance decreased
        assertEquals(10000.0 + expectedInstallment, bankAccount.getBalance()); // Bank received payment

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(accountRepository, times(1)).save(userAccount);
        verify(accountRepository, times(1)).save(bankAccount);
    }

    @Test
    void testProcessInstallment_InsufficientFunds() {
        userAccount.setBalance(50.0);

        Boolean result = transactionService.processInstallment(userAccount, bankAccount, installment);

        assertFalse(result);
        assertEquals(50.0, userAccount.getBalance());
        assertEquals(10000.0, bankAccount.getBalance());

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).save(any());
    }
}

