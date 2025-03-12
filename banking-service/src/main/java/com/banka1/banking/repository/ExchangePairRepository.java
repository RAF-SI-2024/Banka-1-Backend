package com.banka1.banking.repository;

import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.helper.CurrencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangePairRepository extends JpaRepository<ExchangePair, Long> {
    List<ExchangePair> findByBaseCurrencyCode(CurrencyType baseCurrency);
    Optional<ExchangePair> findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType baseCurrency, CurrencyType targetCurrency);
}
