package com.banka1.banking.steps;

import com.banka1.banking.BankingServiceApplication;
import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.ExchangeService;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jms.core.JmsTemplate;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;
import java.util.Optional;

@SpringBootTest
@ContextConfiguration(classes = BankingServiceApplication.class)
public class ExchangeStepDefinitions {

    @InjectMocks
    private ExchangeService exchangeService; // Omogućava injektovanje svih mock-ova u servis

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private ExchangePairRepository exchangePairRepository;

    private ExchangeMoneyTransferDTO dto;
    private boolean validationResult;
    private Long transferId;
    private Map<String, Object> preview;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Inicijalizuje Mockito pre svakog testa
    }

    @Given("accounts {long} and {long} exist, belong to the same user, and have different currencies")
    public void accountsExistDifferentCurrencies(Long fromId, Long toId) {
        Account from = new Account();
        from.setId(fromId);
        from.setOwnerID(1L);
        from.setCurrencyType(CurrencyType.RSD);

        Account to = new Account();
        to.setId(toId);
        to.setOwnerID(1L);
        to.setCurrencyType(CurrencyType.EUR);

        Mockito.when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        Mockito.when(accountRepository.findById(toId)).thenReturn(Optional.of(to));
    }

    @When("creating an exchange transfer from account {long} to account {long} with amount {int}")
    public void createExchangeTransfer(Long fromId, Long toId, int amount) {
        dto = new ExchangeMoneyTransferDTO();
        dto.setAccountFrom(fromId);
        dto.setAccountTo(toId);
        dto.setAmount((double) amount);

        try {
            transferId = exchangeService.createExchangeTransfer(dto);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("transfer should be successfully created")
    public void transferCreated() {
        Assertions.assertNotNull(transferId);
        Mockito.verify(transferRepository).save(Mockito.any());
    }

    @Then("OTP code should be generated and sent to user email")
    public void otpSent() {
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.anyString(), Optional.ofNullable(Mockito.any()));
    }

    @Given("accounts {long} and {long} exist, belong to the same user, but have the same currency")
    public void accountsExistSameCurrencies(Long fromId, Long toId) {
        Account from = new Account();
        from.setId(fromId);
        from.setOwnerID(1L);
        from.setCurrencyType(CurrencyType.EUR);

        Account to = new Account();
        to.setId(toId);
        to.setOwnerID(1L);
        to.setCurrencyType(CurrencyType.EUR);

        Mockito.when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        Mockito.when(accountRepository.findById(toId)).thenReturn(Optional.of(to));
    }

    @When("validating exchange transfer from account {long} to account {long}")
    public void validateExchange(Long fromId, Long toId) {
        dto = new ExchangeMoneyTransferDTO();
        dto.setAccountFrom(fromId);
        dto.setAccountTo(toId);

        validationResult = exchangeService.validateExchangeTransfer(dto);
    }

    @Then("transfer validation should fail")
    public void validationFail() {
        Assertions.assertFalse(validationResult);
    }

    @Given("exchange rate from RSD to EUR is {double}")
    public void exchangeRateExists(double rate) {
        ExchangePair pair = new ExchangePair();
        pair.setExchangeRate(rate);

        // Ispravna inicijalizacija valute
        Currency baseCurrency = new Currency();
        baseCurrency.setCode(CurrencyType.RSD);
        pair.setBaseCurrency(baseCurrency);

        Currency targetCurrency = new Currency();
        targetCurrency.setCode(CurrencyType.EUR);
        pair.setTargetCurrency(targetCurrency);

        Mockito.when(exchangePairRepository.findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType.RSD, CurrencyType.EUR))
                .thenReturn(Optional.of(pair));
    }

    @When("calculating exchange from RSD to EUR for amount {int}")
    public void calculateExchange(int amount) {
        try {
            preview = exchangeService.calculatePreviewExchange("RSD", "EUR", (double) amount);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("exchange preview should return correct converted amount and fee")
    public void previewCorrect() {
        Assertions.assertNotNull(preview);
        Assertions.assertTrue(preview.containsKey("convertedAmount"));
        Assertions.assertTrue(preview.containsKey("fee"));
    }
}
