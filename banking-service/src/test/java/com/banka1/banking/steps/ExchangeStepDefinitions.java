package com.banka1.banking.steps;

import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.*;
import com.banka1.banking.services.ExchangeService;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import java.util.Map;
import java.util.Optional;

public class ExchangeStepDefinitions {

    @Autowired
    private ExchangeService exchangeService;

    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private CurrencyRepository currencyRepository;
    @MockBean
    private JmsTemplate jmsTemplate;
    @MockBean
    private TransferRepository transferRepository;
    @MockBean
    private ExchangePairRepository exchangePairRepository;

    private ExchangeMoneyTransferDTO dto;
    private boolean validationResult;
    private Long transferId;
    private Map<String, Object> preview;
    private Exception exception;

    @Given("accounts {long} and {long} exist, belong to the same user, and have different currencies")
    public void accountsExistDifferentCurrencies(Long fromId, Long toId) {
        Account from = new Account();
        from.setOwnerID(1L);
        from.setCurrencyType(CurrencyType.RSD);
        Account to = new Account();
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
        dto.setAmount((double)amount);

        transferId = exchangeService.createExchangeTransfer(dto);
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
        from.setOwnerID(1L);
        from.setCurrencyType(CurrencyType.EUR);
        Account to = new Account();
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
        Mockito.when(exchangePairRepository.findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType.RSD, CurrencyType.EUR))
                .thenReturn(Optional.of(pair));
    }

    @When("calculating exchange from RSD to EUR for amount {int}")
    public void calculateExchange(int amount) {
        preview = exchangeService.calculatePreviewExchange("RSD", "EUR", (double) amount);
    }

    @Then("exchange preview should return correct converted amount and fee")
    public void previewCorrect() {
        Assertions.assertEquals(100.0, preview.get("convertedAmount"));
        Assertions.assertEquals(1.0, preview.get("fee"));
    }
}
