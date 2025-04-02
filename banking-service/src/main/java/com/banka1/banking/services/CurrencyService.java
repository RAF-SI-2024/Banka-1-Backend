package com.banka1.banking.services;

import com.banka1.banking.dto.ExchangePairDTO;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.ExchangePairRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurrencyService {
    private final ExchangePairRepository exchangePairRepository;
    private final RestTemplate restTemplate;
    private final CurrencyRepository currencyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<CurrencyType> SUPPORTED_CURRENCIES = Arrays.asList(CurrencyType.values());

    public CurrencyService(ExchangePairRepository exchangePairRepository, RestTemplate restTemplate, CurrencyRepository currencyRepository) {
        this.exchangePairRepository = exchangePairRepository;
        this.restTemplate = restTemplate;
        this.currencyRepository = currencyRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")// Svakog dana u ponoc
    public void fetchExchangeRates() {
        exchangePairRepository.deleteAll(); // Brisemo stare podatke

        for (CurrencyType baseCurrencyCode : SUPPORTED_CURRENCIES) {
            String url = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/"
                    + baseCurrencyCode.name().toLowerCase() + ".json";

            try {
                String jsonResponse = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode ratesNode = root.get(baseCurrencyCode.name().toLowerCase());

                if (ratesNode != null) {
                    Currency baseCurrency = currencyRepository.findByCode(baseCurrencyCode)
                            .orElseThrow(() -> new RuntimeException("Base currency " + baseCurrencyCode + " not found"));

                    for (CurrencyType targetCurrencyCode : SUPPORTED_CURRENCIES) {
                        if (!baseCurrencyCode.equals(targetCurrencyCode) &&
                                ratesNode.has(targetCurrencyCode.name().toLowerCase())) {

                            double rate = ratesNode.get(targetCurrencyCode.name().toLowerCase()).asDouble();

                            Currency targetCurrency = currencyRepository.findByCode(targetCurrencyCode)
                                    .orElseThrow(() -> new RuntimeException("Target currency " + targetCurrencyCode + " not found"));

                            ExchangePair pair = new ExchangePair();
                            pair.setBaseCurrency(baseCurrency);
                            pair.setTargetCurrency(targetCurrency);
                            pair.setExchangeRate(rate);
                            pair.setDate(LocalDate.now());

                            exchangePairRepository.save(pair);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch rates for base currency: " + baseCurrencyCode + ", " + e.getMessage());
            }
        }
    }

    public List<ExchangePairDTO> getAllExchangeRates() {
        return exchangePairRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ExchangePairDTO> getExchangeRatesForBaseCurrency(CurrencyType baseCurrency) {
        List<ExchangePair> pairs = exchangePairRepository.findByBaseCurrencyCode(baseCurrency);

        if (pairs.isEmpty()) {
            throw new RuntimeException("Nema dostupnih kurseva za baznu valutu: " + baseCurrency);
        }

        return pairs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ExchangePairDTO mapToDTO(ExchangePair pair) {
        return new ExchangePairDTO(
                pair.getBaseCurrency().getCode().name(),
                pair.getTargetCurrency().getCode().name(),
                pair.getExchangeRate(),
                pair.getDate()
        );
    }

    public List<String> getAvailableCurrencies() {
        return currencyRepository.findAll()
                .stream()
                .map(currency -> currency.getCode().name()) // Pretvara CurrencyType.EUR u "EUR"
                .collect(Collectors.toList());
    }
}
