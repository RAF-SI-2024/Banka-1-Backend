package com.banka1.banking.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.InstallmentsRepository;
import com.banka1.banking.repository.LoanRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.services.LoanService;
import com.banka1.banking.services.TransactionService;
import com.banka1.banking.services.UserServiceCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.*;
import org.mockito.junit.jupiter.MockitoExtension;

@EnableScheduling
@ExtendWith(MockitoExtension.class)
public class LoanService2 {

    @InjectMocks
    private LoanService loanService;  // The service we're testing

    @Mock private InstallmentsRepository installmentsRepository;
    @Mock private TransactionService transactionService;
    @Mock private AccountRepository accountRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private JmsTemplate jmsTemplate;
    @Mock private MessageHelper messageHelper;
    @Mock private ModelMapper modelMapper;
    @Mock private UserServiceCustomer userServiceCustomer;
    @Mock private TransactionRepository transactionRepository;

    @Mock private Account mockAccount;
    @Mock private Installment dueInstallment1;
    @Mock private Installment dueInstallment2;
    @Mock private Loan loan1;
    @Mock private Loan loan2;
    @Mock private Account bankAccount;
    @BeforeEach
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.initMocks(this);

        // Create LoanService instance and inject mocked dependencies

    }
    @Test
    public void testProcessLoanPayments() {
        // Arrange: Mock the necessary data
        when(dueInstallment1.getLoan()).thenReturn(loan1);
        when(dueInstallment2.getLoan()).thenReturn(loan2);
        when(dueInstallment1.getAmount()).thenReturn(1000.0);
        when(dueInstallment2.getAmount()).thenReturn(1500.0);
        when(dueInstallment1.getLoan().getAccount()).thenReturn(mockAccount);
        when(dueInstallment2.getLoan().getAccount()).thenReturn(mockAccount);

        // Assume that installmentsRepository returns due installments
        when(installmentsRepository.getDueInstallments(anyLong())).thenReturn(Arrays.asList(dueInstallment1, dueInstallment2));

        // Assume that processInstallment returns true for one installment and false for another
        when(transactionService.processInstallment(mockAccount, bankAccount, dueInstallment1)).thenReturn(true);
        when(transactionService.processInstallment(mockAccount, bankAccount, dueInstallment2)).thenReturn(false);

        // Mock the getBankAccount() method
        when(loanService.getBankAccount(any())).thenReturn(bankAccount);

        // Act: Call the processLoanPayments method (simulating cron job execution)
        loanService.processLoanPayments();

        // Assert: Verify that the correct methods were called and check the results
        verify(transactionService, times(1)).processInstallment(mockAccount, bankAccount, dueInstallment1);
        verify(transactionService, times(1)).processInstallment(mockAccount, bankAccount, dueInstallment2);

        // Verify that installments are saved after being processed
        verify(installmentsRepository, times(2)).save(any(Installment.class));

        // Verify that installment 1 is marked as paid, and installment 2 has a retry date
        verify(dueInstallment1, times(1)).setIsPaid(true);
        verify(dueInstallment2, times(1)).setIsPaid(false);
        verify(dueInstallment1, times(1)).setActualDueDate(anyLong());
        verify(dueInstallment2, times(1)).setRetryDate(anyLong());
    }
}
