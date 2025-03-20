package com.banka1.banking.steps;

import com.banka1.banking.models.*;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.TransactionService;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


import java.util.Optional;

@SpringBootTest
@ContextConfiguration(classes = TransactionService.class)
public class TransactionStepDefinitions {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CurrencyRepository currencyRepository;

    private Transfer transfer;
    private Account senderAccount;
    private Account receiverAccount;
    private Installment installment;
    private Exception exception;
    private Double calculatedInstallment;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Ispravlja NullPointerException na mock-ovima
    }

    @Given("internal transfer with id {long} is pending and sender has sufficient balance")
    public void internalTransferPendingSufficientFunds(Long transferId) {
        senderAccount = new Account();
        senderAccount.setBalance(1000.0);

        receiverAccount = new Account();
        receiverAccount.setBalance(500.0);

        transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setType(TransferType.INTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setAmount(200.0);
        transfer.setFromAccountId(senderAccount);
        transfer.setToAccountId(receiverAccount);

        Mockito.when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
    }

    @Given("internal transfer with id {long} is pending and sender has insufficient balance")
    public void internalTransferPendingInsufficientFunds(Long transferId) {
        senderAccount = new Account();
        senderAccount.setBalance(100.0);  // insufficient balance

        transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setType(TransferType.INTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setAmount(200.0);
        transfer.setFromAccountId(senderAccount);

        Mockito.when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
    }

    @When("the internal transfer with id {long} is processed")
    public void processInternalTransfer(Long transferId) {
        try {
            transactionService.processInternalTransfer(transferId);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("internal transfer should be marked as COMPLETED")
    public void internalTransferCompleted() {
        Assertions.assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
    }

    @Then("both accounts should be updated correctly")
    public void accountsUpdatedCorrectly() {
        Assertions.assertEquals(800.0, senderAccount.getBalance());
        Assertions.assertEquals(700.0, receiverAccount.getBalance());
    }

    @Then("internal transfer should fail with message {string}")
    public void internalTransferFailMessage(String message) {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Then("transfer status should be marked as FAILED")
    public void transferMarkedFailed() {
        Assertions.assertEquals(TransferStatus.FAILED, transfer.getStatus());
    }

    @Then("accounts and transactions should be updated")
    public void accountsAndTransactionsShouldBeUpdated() {
        // Provera da li su oba računa ažurirana
        Assertions.assertTrue(senderAccount.getBalance() < 10000.0); // Proveravamo da li je iznos skinut
        Assertions.assertTrue(receiverAccount.getBalance() > 5000.0); // Proveravamo da li je banka primila uplatu

        // Provera da li su transakcije sačuvane
        Mockito.verify(transactionRepository, Mockito.atLeastOnce()).save(Mockito.any(Transaction.class));
    }


    @Given("external transfer with id {long} is pending and sender has sufficient balance")
    public void externalTransferPendingSufficientFunds(Long transferId) {
        senderAccount = new Account();
        senderAccount.setBalance(2000.0);

        receiverAccount = new Account();
        receiverAccount.setBalance(500.0);

        transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setType(TransferType.EXTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setAmount(500.0);
        transfer.setFromAccountId(senderAccount);
        transfer.setToAccountId(receiverAccount);

        Mockito.when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
    }

    @When("calculating installment for loan amount {int} with annual interest rate {int} and {int} installments")
    public void calculateInstallment(int loanAmount, int annualRate, int installments) {
        calculatedInstallment = transactionService.calculateInstallment((double)loanAmount, (double)annualRate, installments);
    }

    @Then("the installment amount should be correctly calculated as {double}")
    public void installmentAmountCalculatedCorrectly(Double expectedInstallment) {
        Assertions.assertEquals(expectedInstallment, calculatedInstallment, 0.01);
    }

    @Given("customer has sufficient funds for installment")
    public void customerHasFunds() {
        senderAccount = new Account();
        senderAccount.setBalance(10000.0);
        receiverAccount = new Account();
        receiverAccount.setBalance(5000.0);

        installment = new Installment();
        Loan loan = new Loan();
        loan.setLoanAmount(1200.0);
        loan.setNumberOfInstallments(12);
        installment.setLoan(loan);
        installment.setInterestRate(6.0);
    }

    @When("processing loan installment payment")
    public void processLoanInstallment() {
        transactionService.processInstallment(senderAccount, receiverAccount, installment);
    }

    @Then("loan installment should be processed successfully")
    public void loanInstallmentProcessed() {
        Assertions.assertTrue(senderAccount.getBalance() < 10000.0);
    }

    @Given("customer has insufficient funds for installment")
    public void customerInsufficientFunds() {
        senderAccount = new Account();
        senderAccount.setBalance(10.0); // insufficient
    }

    @Then("loan installment payment should fail")
    public void loanInstallmentFail() {
        boolean result = transactionService.processInstallment(senderAccount, receiverAccount, installment);
        Assertions.assertFalse(result);
    }


    @Then("transactions should be created successfully")
    public void transactionsShouldBeCreatedSuccessfully() {
        // Proveravamo da li su transakcije uspešno kreirane
        Mockito.verify(transactionRepository, Mockito.atLeast(1)).save(Mockito.any(Transaction.class));
    }

    @When("the external transfer with id {long} is processed")
    public void theExternalTransferWithIdIsProcessed(Long transferId) {
        try {
            transactionService.processExternalTransfer(transferId);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("external transfer should be marked as COMPLETED")
    public void externalTransferShouldBeMarkedAsCompleted() {
        Assertions.assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
    }

    @Then("accounts should be updated correctly")
    public void accountsShouldBeUpdatedCorrectly() {
        Assertions.assertTrue(senderAccount.getBalance() < 2000.0); // Proveravamo da je iznos skinut
        Assertions.assertTrue(receiverAccount.getBalance() > 500.0); // Proveravamo da je primalac dobio novac
    }

    @Given("external transfer with id {long} is pending and sender has insufficient balance")
    public void externalTransferPendingInsufficientFunds(Long transferId) {
        senderAccount = new Account();
        senderAccount.setBalance(50.0);  // Nedovoljno sredstava

        receiverAccount = new Account();
        receiverAccount.setBalance(500.0);

        transfer = new Transfer();
        transfer.setId(transferId);
        transfer.setType(TransferType.EXTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setAmount(500.0);
        transfer.setFromAccountId(senderAccount);
        transfer.setToAccountId(receiverAccount);

        Mockito.when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
    }

    @Then("external transfer should fail with message {string}")
    public void externalTransferShouldFailWithMessage(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains(expectedMessage));
    }



}