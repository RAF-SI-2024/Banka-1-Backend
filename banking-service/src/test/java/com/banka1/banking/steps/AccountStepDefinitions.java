package com.banka1.banking.steps;

import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.services.AccountService;
import com.banka1.banking.services.UserServiceCustomer;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import java.util.Optional;

public class AccountStepDefinitions {

    @Autowired
    private AccountService accountService;

    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private JmsTemplate jmsTemplate;
    @MockBean
    private UserServiceCustomer userServiceCustomer;
    @MockBean
    private TransactionRepository transactionRepository;

    private Account account;
    private Exception exception;

    @Given("customer with id {long} exists")
    public void customerExists(Long customerId) {
        Mockito.when(userServiceCustomer.getCustomerById(customerId)).thenReturn(Mockito.mock(com.banka1.banking.dto.CustomerDTO.class));
    }

    @When("employee with id {long} creates a {word} account in {word} currency for customer {long}")
    public void createAccount(Long employeeId, String type, String currency, Long customerId) {
        CreateAccountDTO dto = new CreateAccountDTO();
        dto.setType(AccountType.valueOf(type));
        dto.setCurrency(CurrencyType.valueOf(currency));
        dto.setOwnerID(customerId);
        dto.setCreateCard(false);

        try {
            account = accountService.createAccount(dto, employeeId);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("the account should be created successfully")
    public void accountCreatedSuccessfully() {
        Assertions.assertNotNull(account);
        Mockito.verify(accountRepository).save(Mockito.any(Account.class));
    }

    @Then("notification email should be sent to customer")
    public void emailSentToCustomer() {
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.anyString(), Optional.ofNullable(Mockito.any()));
    }

    @Then("account creation should fail with message {string}")
    public void accountCreationShouldFail(String message) {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Given("account with id {long} exists")
    public void accountExists(Long accountId) {
        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(new Account()));
    }

    @When("retrieving transactions for account {long}")
    public void retrieveTransactions(Long accountId) {
        accountService.getTransactionsForAccount(accountId);
    }

    @Then("transactions should be successfully returned")
    public void transactionsReturnedSuccessfully() {
        Mockito.verify(transactionRepository).findByFromAccountId(Mockito.any(Account.class));
        Mockito.verify(transactionRepository).findByToAccountId(Mockito.any(Account.class));
    }
}
