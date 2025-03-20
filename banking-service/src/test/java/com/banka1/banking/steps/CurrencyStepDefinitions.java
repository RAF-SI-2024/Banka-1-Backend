package com.banka1.banking.steps;

import com.banka1.banking.BankingServiceApplication;
import com.banka1.banking.dto.ExchangePairDTO;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.ExchangePairRepository;
import com.banka1.banking.services.CurrencyService;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;

import java.time.LocalDate;
import java.util.*;

@SpringBootTest
@ContextConfiguration(classes = BankingServiceApplication.class)
public class CurrencyStepDefinitions {

    @InjectMocks
    private CurrencyService currencyService; // Pravilno inicijalizuje servis sa mock dependencijama

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangePairRepository exchangePairRepository;

    private List<ExchangePairDTO> exchangePairDTOList;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Inicijalizuje Mockito pre svakog testa
    }

    @Given("external exchange rates API is available")
    public void externalExchangeRatesApiIsAvailable() {
        String apiResponse = "{\"rates\":{\"EUR\":117.5,\"USD\":110.0}}";
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(apiResponse);
    }

    @Given("external exchange rates API is not available")
    public void externalExchangeRatesApiIsNotAvailable() {
        Mockito.when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(String.class))).thenThrow(new RuntimeException("API unavailable"));
    }

    @Given("currencies RSD, EUR, USD exist in the database")
    public void currenciesExistInDatabase() {
        Currency rsd = new Currency();
        rsd.setCode(CurrencyType.RSD);

        Currency eur = new Currency();
        eur.setCode(CurrencyType.EUR);

        Currency usd = new Currency();
        usd.setCode(CurrencyType.USD);

        Mockito.when(currencyRepository.findByCode(CurrencyType.RSD)).thenReturn(Optional.of(rsd));
        Mockito.when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(eur));
        Mockito.when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(usd));
    }

    @When("the scheduled task to fetch exchange rates is triggered")
    public void scheduledTaskTriggered() {
        try {
            currencyService.fetchExchangeRates();
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("exchange rates should be successfully saved into the database")
    public void exchangeRatesSavedSuccessfully() {
        Mockito.verify(exchangePairRepository, Mockito.atLeast(1)).save(Mockito.any(ExchangePair.class));
        Assertions.assertNull(exception);
    }

    @Then("no exchange rates should be saved")
    public void noExchangeRatesSaved() {
        Mockito.verify(exchangePairRepository, Mockito.never()).save(Mockito.any(ExchangePair.class));
    }

    @Then("appropriate error message should be logged")
    public void errorMessageLogged() {
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getMessage().contains("API unavailable"));
    }

    @Given("there are exchange rates in the database")
    public void exchangeRatesInDatabase() {
        Currency baseCurrency = new Currency();
        baseCurrency.setCode(CurrencyType.RSD);

        Currency targetCurrency = new Currency();
        targetCurrency.setCode(CurrencyType.EUR);

        ExchangePair pairEUR = new ExchangePair();
        pairEUR.setBaseCurrency(baseCurrency);
        pairEUR.setTargetCurrency(targetCurrency);
        pairEUR.setExchangeRate(117.5);
        pairEUR.setDate(LocalDate.now());

        Mockito.when(exchangePairRepository.findAll()).thenReturn(Collections.singletonList(pairEUR));
        Mockito.when(exchangePairRepository.findByBaseCurrencyCode(CurrencyType.RSD)).thenReturn(Collections.singletonList(pairEUR));
    }

    @When("user requests all exchange rates")
    public void userRequestsAllExchangeRates() {
        exchangePairDTOList = currencyService.getAllExchangeRates();
    }

    @Then("all exchange rates should be returned successfully")
    public void allExchangeRatesReturned() {
        Assertions.assertNotNull(exchangePairDTOList);
        Assertions.assertFalse(exchangePairDTOList.isEmpty());
        Assertions.assertEquals(1, exchangePairDTOList.size());
    }

    @When("user requests exchange rates for a non-existing base currency {string}")
    public void userRequestsNonExistingBaseCurrency(String baseCurrency) {
        try {
            exchangePairDTOList = currencyService.getExchangeRatesForBaseCurrency(CurrencyType.valueOf(baseCurrency));
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("exchange rate retrieval should fail with error {string}")
    public void exchangeRateRetrievalFailWithError(String expectedError) {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(expectedError, exception.getMessage());
    }
}
