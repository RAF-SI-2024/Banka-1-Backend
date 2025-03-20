package com.banka1.banking.steps;

import com.banka1.banking.BankingServiceApplication;
import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.services.AccountService;
import com.banka1.banking.services.UserServiceCustomer;
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

import java.util.Optional;

@SpringBootTest
@ContextConfiguration(classes = BankingServiceApplication.class)
public class AccountStepDefinitions {

    @InjectMocks
    private AccountService accountService; // Osigurava da koristi @Mock instance

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private UserServiceCustomer userServiceCustomer;
    @Mock
    private TransactionRepository transactionRepository;

    private Account account;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this); // Omogućava @MockBean da radi

        // 📌 Osigurava da svi .save() vraćaju isti objekat umesto null
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }


    @Given("customer with id {long} exists")
    public void customerExists(Long customerId) {
        com.banka1.banking.dto.CustomerDTO mockCustomer = new com.banka1.banking.dto.CustomerDTO();
        mockCustomer.setId(customerId);
        mockCustomer.setFirstName("Test");
        mockCustomer.setLastName("User");
        mockCustomer.setEmail("test@example.com");

        Mockito.when(userServiceCustomer.getCustomerById(customerId)).thenReturn(mockCustomer);
    }


    @When("employee with id {long} creates a {word} account in {word} currency for customer {long}")
    public void createAccount(Long employeeId, String type, String currency, Long customerId) {
        CreateAccountDTO dto = new CreateAccountDTO();
        dto.setType(AccountType.valueOf(type));
        dto.setCurrency(CurrencyType.valueOf(currency));
        dto.setOwnerID(customerId);
        dto.setCreateCard(false);

        // 🛠️ Kreiranje mock naloga koji će biti sačuvan u bazi
        Account mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setType(AccountType.valueOf(type));
        mockAccount.setCurrencyType(CurrencyType.valueOf(currency));
        mockAccount.setOwnerID(customerId);

        try {
            account = accountService.createAccount(dto, employeeId);
        } catch (Exception e) {
            exception = e;
        }
    }


    @Then("the account should be created successfully")
    public void accountCreatedSuccessfully() {
        Assertions.assertNotNull(account, "Account object should not be null");
        Assertions.assertEquals(AccountType.CURRENT, account.getType());
        Assertions.assertEquals(CurrencyType.RSD, account.getCurrencyType());

        // 🛠️ Proveravamo da li je Mockito pozvao `save`
        Mockito.verify(accountRepository, Mockito.times(1)).save(Mockito.any(Account.class));
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
        Account mockAccount = new Account();
        mockAccount.setId(accountId);
        mockAccount.setBalance(1000.0);

        Mockito.when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount));
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

    @When("employee with id {long} tries to create a CURRENT account in EUR currency for customer {long}")
    public void createCurrentAccountInEur(Long employeeId, Long customerId) {
        CreateAccountDTO dto = new CreateAccountDTO();
        dto.setType(AccountType.CURRENT);
        dto.setCurrency(CurrencyType.EUR);
        dto.setOwnerID(customerId);
        dto.setCreateCard(false);

        try {
            account = accountService.createAccount(dto, employeeId);
        } catch (Exception e) {
            exception = e;
        }
    }

}
