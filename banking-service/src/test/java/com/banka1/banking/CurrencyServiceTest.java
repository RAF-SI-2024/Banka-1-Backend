package com.banka1.banking;

import com.banka1.banking.dto.ExchangePairDTO;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.ExchangePairRepository;
import com.banka1.banking.services.CurrencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private ExchangePairRepository exchangePairRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    @DisplayName("Test: fetchExchangeRates() - Uspešno generisanje kursne liste")
    void testFetchExchangeRates_Success() {
        // Mock API response
        Map<String, Double> mockRates = Map.of(
                "EUR", 117.3,
                "USD", 108.5
        );

        // Simulacija API poziva
        when(restTemplate.getForObject(eq("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/rsd.json"), eq(String.class)))
                .thenReturn("{\"rsd\":{\"eur\":117.3,\"usd\":108.5}}");

        // Mock baze valuta
        Currency rsd = new Currency();
        rsd.setCode(CurrencyType.RSD);

        Currency eur = new Currency();
        eur.setCode(CurrencyType.EUR);

        Currency usd = new Currency();
        usd.setCode(CurrencyType.USD);

        when(currencyRepository.findByCode(CurrencyType.RSD)).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(usd));

        // Poziv testirane metode
        currencyService.fetchExchangeRates();

        // Provera da li su kursni parovi sačuvani u bazi
        verify(exchangePairRepository, times(1)).save(argThat(pair ->
                pair.getBaseCurrency().getCode() == CurrencyType.RSD &&
                        pair.getTargetCurrency().getCode() == CurrencyType.EUR &&
                        pair.getExchangeRate() == 117.3
        ));

        verify(exchangePairRepository, times(1)).save(argThat(pair ->
                pair.getBaseCurrency().getCode() == CurrencyType.RSD &&
                        pair.getTargetCurrency().getCode() == CurrencyType.USD &&
                        pair.getExchangeRate() == 108.5
        ));
    }

    @Test
    @DisplayName("Test: getExchangeRatesForBaseCurrency() - Prikaz kursne liste u odnosu na EUR")
    void testGetExchangeRatesForBaseCurrency_Success() {
        // Mock podataka iz baze
        Currency eur = new Currency();
        eur.setCode(CurrencyType.EUR);

        Currency usd = new Currency();
        usd.setCode(CurrencyType.USD);

        ExchangePair eurToUsd = new ExchangePair();
        eurToUsd.setBaseCurrency(eur);
        eurToUsd.setTargetCurrency(usd);
        eurToUsd.setExchangeRate(1.1);
        eurToUsd.setDate(LocalDate.now());

        when(exchangePairRepository.findByBaseCurrencyCode(CurrencyType.EUR))
                .thenReturn(List.of(eurToUsd));

        // Poziv metode
        List<ExchangePairDTO> result = currencyService.getExchangeRatesForBaseCurrency(CurrencyType.EUR);

        // Provera rezultata
        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).getTargetCurrency());
        assertEquals("EUR", result.get(0).getBaseCurrency());
        assertEquals(1.1, result.get(0).getExchangeRate());
    }

    @Test
    @DisplayName("Test: testGetAvailableCurrencies() - Provera dobijanja svih dostupnih valuta")
    void testGetAvailableCurrencies() {
        // Priprema podataka
        Currency eur = new Currency();
        eur.setCode(CurrencyType.EUR);

        Currency usd = new Currency();
        usd.setCode(CurrencyType.USD);

        when(currencyRepository.findAll()).thenReturn(List.of(eur, usd));

        // Poziv metode
        List<String> result = currencyService.getAvailableCurrencies();

        // Provera
        assertEquals(2, result.size());
        assertTrue(result.contains("EUR"));
        assertTrue(result.contains("USD"));
    }

    @Test
    @DisplayName("Test: getAllExchangeRates() - Provera dobijanja svih kurseva")
    void testGetAllExchangeRates() {
        // Mock podaci
        Currency rsd = new Currency();
        rsd.setCode(CurrencyType.RSD);

        Currency eur = new Currency();
        eur.setCode(CurrencyType.EUR);

        ExchangePair pair = new ExchangePair();
        pair.setBaseCurrency(rsd);
        pair.setTargetCurrency(eur);
        pair.setExchangeRate(117.3);
        pair.setDate(LocalDate.now());

        when(exchangePairRepository.findAll()).thenReturn(List.of(pair));

        // Poziv metode
        List<ExchangePairDTO> result = currencyService.getAllExchangeRates();

        // Provera
        assertEquals(1, result.size());
        assertEquals("RSD", result.get(0).getBaseCurrency());
        assertEquals("EUR", result.get(0).getTargetCurrency());
        assertEquals(117.3, result.get(0).getExchangeRate());
    }

    @Test
    @DisplayName("Test: fetchExchangeRates() - API vraća prazan odgovor")
    void testFetchExchangeRates_EmptyResponse() {
        // Simulacija praznog API odgovora
        when(restTemplate.getForObject(eq("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/rsd.json"), eq(String.class)))
                .thenReturn("{\"rsd\":{}}");

        // Pokretanje metode
        currencyService.fetchExchangeRates();

        // Provera da se ništa ne čuva u bazi jer API nije vratio podatke
        verify(exchangePairRepository, never()).save(any(ExchangePair.class));
    }
}
