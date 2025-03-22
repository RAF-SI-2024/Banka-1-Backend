package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BankAccountUtils {

    private final AccountRepository accountRepository;

    public Account getBankAccountForCurrency(CurrencyType currencyType){
        return accountRepository
                .findByTypeAndCurrencyType(AccountType.BANK,currencyType)
                .orElseThrow(() -> new RuntimeException("Račun banke za valutu " + currencyType + " nije pronađen."));
    }

    public Account getCountryAccountForCurrency(CurrencyType currencyType){
        return accountRepository
                .findByTypeAndCurrencyType(AccountType.COUNTRY,currencyType)
                .orElseThrow(() -> new RuntimeException("Račun drzave za valutu " + currencyType + " nije pronađen."));
    }

}
