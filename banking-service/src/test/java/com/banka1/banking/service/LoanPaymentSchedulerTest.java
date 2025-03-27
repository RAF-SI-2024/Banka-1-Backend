package com.banka1.banking.service;

import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.models.*;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.BankAccountUtils;
import com.banka1.banking.services.LoanService;
import com.banka1.banking.services.TransactionService;
import com.banka1.banking.services.TransferService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanPaymentSchedulerTest {

	@Mock
	private InstallmentsRepository installmentsRepository;

	@Mock
	private TransferService transferService;

	@Mock
	private BankAccountUtils bankAccountUtils;

	@Mock
	private LoanRepository loanRepository;

	@Mock
	private TransactionService transactionService;

	@InjectMocks
	private LoanService loanService;

	private Installment installment1;
	private Installment installment2;
	private Account userAccount;
	private Account bankAccount;
	private Loan loan;
	private Transfer transfer;

	@BeforeEach
	void setUp() {
		userAccount = new Account();
		userAccount.setBalance(5000.0);
		userAccount.setCurrencyType(CurrencyType.AUD);

		bankAccount = new Account();
		bankAccount.setBalance(10000.0);
		bankAccount.setCurrencyType(CurrencyType.AUD);

		loan = new Loan();
		loan.setLoanAmount(1000.0);
		loan.setEffectiveRate(4.45);
		loan.setInterestType(InterestType.FIXED);
		loan.setLoanType(LoanType.CASH);
		loan.setNumberOfInstallments(12);
		loan.setAccount(userAccount);
		loan.setNextPaymentDate(LocalDate.of(1, 1, 1));

		installment1 = new Installment();
		installment1.setId(1L);
		installment1.setInstallmentNumber(0);
		installment1.setLoan(loan);
		installment1.setCurrencyType(CurrencyType.AUD);
		installment1.setInterestRate(5.0); // 5% interest
		installment1.setAttemptCount(0);
		installment1.setIsPaid(false);

		installment2 = new Installment();
		installment2.setId(2L);
		installment2.setInstallmentNumber(0);
		installment2.setLoan(loan);
		installment2.setCurrencyType(CurrencyType.AUD);
		installment2.setInterestRate(5.0); // 5% interest
		installment2.setAttemptCount(0);
		installment2.setIsPaid(false);

		transfer = new Transfer();
		transfer.setId(1L);
	}

	@Test
	void testProcessLoanPaymentsSuccess() {
		when(installmentsRepository.getDueInstallments(any(LocalDate.class))).thenReturn(Arrays.asList(installment1,
				installment2));
		when(bankAccountUtils.getBankAccountForCurrency(CurrencyType.AUD)).thenReturn(bankAccount);
		when(transferService.validateMoneyTransfer(any(MoneyTransferDTO.class))).thenReturn(true);
		when(transferService.createMoneyTransferEntity(eq(userAccount), eq(bankAccount), any(MoneyTransferDTO.class))).thenReturn(transfer);
		when(loanRepository.save(any(Loan.class))).then(invocationOnMock -> invocationOnMock.getArgument(0));

		installment1.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment1.getInterestRate(), loan.getNumberOfInstallments()));
		installment2.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment2.getInterestRate(), loan.getNumberOfInstallments()));

		loanService.processLoanPayments();

		assertTrue(installment1.getIsPaid());
		assertTrue(installment2.getIsPaid());
		assertNotNull(installment1.getActualDueDate());
		assertNotNull(installment2.getActualDueDate());

		verify(installmentsRepository).save(installment1);
		verify(installmentsRepository).save(installment2);
	}

	@Test
	void testProcessLoanPaymentsFailed() {
		when(installmentsRepository.getDueInstallments(any(LocalDate.class))).thenReturn(Arrays.asList(installment1,
				installment2));
		when(bankAccountUtils.getBankAccountForCurrency(CurrencyType.AUD)).thenReturn(bankAccount);

		userAccount.setBalance(50.0);

		installment1.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment1.getInterestRate(), loan.getNumberOfInstallments()));
		installment2.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment2.getInterestRate(), loan.getNumberOfInstallments()));

		loanService.processLoanPayments();

		assertFalse(installment1.getIsPaid());
		assertFalse(installment2.getIsPaid());
		assertNotNull(installment1.getRetryDate());
		assertNotNull(installment2.getRetryDate());

		verify(installmentsRepository).save(installment1);
		verify(installmentsRepository).save(installment2);
	}

	@Test
	void testProcessLoanPaymentsInvalidTransfer() {
		when(installmentsRepository.getDueInstallments(any(LocalDate.class))).thenReturn(Arrays.asList(installment1,
				installment2));
		when(transferService.validateMoneyTransfer(any(MoneyTransferDTO.class))).thenReturn(false);
		when(bankAccountUtils.getBankAccountForCurrency(CurrencyType.AUD)).thenReturn(bankAccount);

		installment1.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment1.getInterestRate(), loan.getNumberOfInstallments()));
		installment2.setAmount(loanService.calculateInstallment(loan.getLoanAmount(), installment2.getInterestRate(), loan.getNumberOfInstallments()));

		loanService.processLoanPayments();

		assertFalse(installment1.getIsPaid());
		assertFalse(installment2.getIsPaid());
		assertNull(installment1.getRetryDate());
		assertNull(installment2.getRetryDate());
	}

	@Test
	void testProcessDueInstallments_NoInstallmentsFound() {
		when(installmentsRepository.getDueInstallments(any(LocalDate.class))).thenReturn(Collections.emptyList());

		loanService.processLoanPayments();

		verify(installmentsRepository, never()).save(any());
	}

	@Test
	void testCalculateInstallment_WithInterest() {
		Double loanAmount = 1000.0;
		Double annualInterestRate = 12.0; // 12%
		Integer numberOfInstallments = 12;

		Double installment = loanService.calculateInstallment(loanAmount, annualInterestRate / 12, numberOfInstallments);

		// Expected monthly payment with 1% monthly interest rate
		// Using formula: P * [r(1+r)^n]/[(1+r)^n-1]
		assertTrue(installment > 83.0 && installment < 89.0); // Approximate value should be around 88.85
	}

	@Test
	void testCalculateInstallment_ZeroInterest() {
		Double loanAmount = 1000.0;
		Double annualInterestRate = 0.0;
		Integer numberOfInstallments = 10;

		Double installment = loanService.calculateInstallment(loanAmount, annualInterestRate, numberOfInstallments);

		assertEquals(100.0, installment);
	}
}