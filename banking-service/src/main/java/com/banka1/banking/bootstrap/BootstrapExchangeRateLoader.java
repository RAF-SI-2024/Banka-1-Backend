package com.banka1.banking.bootstrap;

import com.banka1.banking.services.CurrencyService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapExchangeRateLoader implements CommandLineRunner {

    private final CurrencyService currencyService;

    public BootstrapExchangeRateLoader(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Fetching exchange rates on startup ===");
        currencyService.fetchExchangeRates();
        System.out.println("=== Exchange rates fetched successfully ===");
    }
}
