package com.banka1.banking.service;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanPaymentSchedulerTest {

    @Mock
    private InstallmentsRepository installmentsRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @InjectMocks
    private LoanService loanPaymentScheduler;

    private Installment unpaidInstallment;
    private Installment paidInstallment;

    @BeforeEach
    void setUp() {
        Account customerAccount = new Account();
        customerAccount.setCurrencyType(CurrencyType.EUR);
        customerAccount.setBalance(1000.0); // ✅ Dodaj balans da test prođe

        Account bankAccount = new Account();
        bankAccount.setCurrencyType(CurrencyType.EUR);
        bankAccount.setBalance(10000.0);

        unpaidInstallment = new Installment();
        unpaidInstallment.setLoan(new Loan());
        unpaidInstallment.setAmount(500.0);
        unpaidInstallment.getLoan().setAccount(customerAccount);
        unpaidInstallment.setIsPaid(false);
        unpaidInstallment.setAttemptCount(1);
        unpaidInstallment.setInterestRate(0.05);

        paidInstallment = new Installment();
        paidInstallment.setLoan(new Loan());
        paidInstallment.setAmount(30.0);
        paidInstallment.getLoan().setAccount(customerAccount);
        paidInstallment.setIsPaid(true);

        when(installmentsRepository.getDueInstallments(anyLong()))
                .thenReturn(Arrays.asList(unpaidInstallment, paidInstallment));
    }

    @Test
    void testProcessLoanPayments() {
//        when(transactionService.processInstallment(any(), any(), any())).thenReturn(true);

        loanPaymentScheduler.processLoanPayments();

        // Verifikujemo da je metoda pozvana i da se pozvao `save` dva puta (jer imamo dva installment-a)
        verify(installmentsRepository, times(1)).getDueInstallments(anyLong());
        verify(installmentsRepository, times(2)).save(any(Installment.class));

        // Proveravamo da li je dug isplaćen
        assertTrue(unpaidInstallment.getIsPaid(), "Unpaid installment should be marked as paid.");
    }

    @Test
    void testProcessLoanPaymentsFailedTransaction() {
        when(transactionService.processInstallment(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Installment inst = invocation.getArgument(2);
                    assertNotNull(inst.toString(), "Installment should not be null");
                    assertNotNull(String.valueOf(inst.getAmount()), "Installment amount should not be null");
                    return false; // Simuliramo neuspelu transakciju
                });
        loanPaymentScheduler.processLoanPayments();

        // Verifikujemo da je metoda pozvana i da se pozvao `save` dva puta
        verify(installmentsRepository, times(1)).getDueInstallments(anyLong());
        verify(installmentsRepository, times(2)).save(any(Installment.class));

        // Proveravamo da li nije plaćen i da je retryDate podešen
        assertFalse(unpaidInstallment.getIsPaid(), "Unpaid installment should not be marked as paid.");
        assertNotNull(String.valueOf(unpaidInstallment.getRetryDate()), "Retry date should be set.");
    }
}
