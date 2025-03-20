package com.banka1.banking.steps;

import com.banka1.banking.BankingServiceApplication;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@ContextConfiguration(classes = BankingServiceApplication.class)
public class LoanServiceStepDefinitions {

    @InjectMocks
    private LoanService loanService;  // Automatski injektuje mockove

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InstallmentsRepository installmentsRepository;
    @Mock
    private UserServiceCustomer userServiceCustomer;
    @Mock
    private TransactionService transactionService;
    @Mock
    private JmsTemplate jmsTemplate;

    private Loan loan;
    private Account account;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Ispravlja null mock dependency problem
    }

    @Given("customer account with id {long} exists")
    public void customerAccountExists(Long accountId) {
        account = new Account();
        account.setId(accountId);
        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    }

    @When("customer requests a {word} loan with amount {int} and {int} installments")
    public void customerRequestsLoan(String type, int amount, int installments) {
        CreateLoanDTO dto = new CreateLoanDTO();
        dto.setAccountId(account.getId());
        dto.setLoanAmount((double) amount);
        dto.setNumberOfInstallments(installments);
        dto.setLoanType(LoanType.valueOf(type));

        try {
            loan = loanService.createLoan(dto);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("loan should be created with status {string}")
    public void loanCreatedWithStatus(String status) {
        Assertions.assertNotNull(loan);
        Assertions.assertEquals(PaymentStatus.valueOf(status), loan.getPaymentStatus());
    }

    @Then("loan creation should fail with message {string}")
    public void loanCreationFailWithMessage(String expectedMessage) {
        Assertions.assertNull(loan);
        Assertions.assertNotNull(exception);
        Assertions.assertFalse(exception.getMessage().contains(expectedMessage));
    }

    @Given("a pending loan request with id {long} exists")
    public void pendingLoanRequestExists(Long loanId) {
        loan = new Loan();
        loan.setId(loanId);
        loan.setPaymentStatus(PaymentStatus.PENDING);
        loan.setAccount(account);
        Mockito.when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
    }

    @When("employee approves loan request with id {long}")
    public void employeeApprovesLoan(Long loanId) {
        LoanUpdateDTO dto = new LoanUpdateDTO();
        dto.setApproved(true);
        loan = loanService.updateLoanRequest(loanId, dto);
    }

    @When("employee rejects loan request with id {long} with reason {string}")
    public void employeeRejectsLoan(Long loanId, String reason) {
        LoanUpdateDTO dto = new LoanUpdateDTO();
        dto.setApproved(false);
        dto.setReason(reason);
        loan = loanService.updateLoanRequest(loanId, dto);
    }

    @Then("loan status should be updated to {string}")
    public void loanStatusUpdated(String status) {
        Assertions.assertEquals(PaymentStatus.valueOf(status), loan.getPaymentStatus());
    }

    @Then("customer should receive notification {string}")
    public void customerReceivesNotification(String message) {
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.anyString(), Mockito.contains(message));
    }

    @Given("there are installments due for today")
    public void installmentsDueToday() {
        Installment installment = new Installment();
        installment.setLoan(loan);
        Mockito.when(installmentsRepository.getDueInstallments(Mockito.anyLong())).thenReturn(List.of(installment));
        Mockito.when(transactionService.processInstallment(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
    }

    @Given("there are no installments due for today")
    public void noInstallmentsDueToday() {
        Mockito.when(installmentsRepository.getDueInstallments(Mockito.anyLong())).thenReturn(List.of());
    }

    @When("scheduled loan installment processing is triggered")
    public void scheduledInstallmentProcessingTriggered() {
        try {
            loanService.processLoanPayments();
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("all due installments should be processed and marked as paid")
    public void dueInstallmentsProcessed() {
        Mockito.verify(installmentsRepository, Mockito.atLeastOnce()).save(Mockito.any(Installment.class));
        Assertions.assertNull(exception);
    }

    @Then("loan installment processing should fail with message {string}")
    public void installmentProcessingFail(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @When("customer requests a MORTGAGE loan with amount {int} and invalid {int} installments")
    public void customerRequestsMortgageLoanWithInvalidInstallments(int amount, int installments) {
        CreateLoanDTO dto = new CreateLoanDTO();
        dto.setAccountId(account.getId());
        dto.setLoanAmount((double) amount);
        dto.setNumberOfInstallments(installments);
        dto.setLoanType(LoanType.MORTGAGE);

        try {
            loan = loanService.createLoan(dto);
        } catch (Exception e) {
            exception = e;
        }
    }

}
