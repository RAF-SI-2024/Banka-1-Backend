package com.banka1.banking.repository;

import com.banka1.banking.models.Currency;
import com.banka1.banking.models.helper.CurrencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    Optional<Currency> findByCode(CurrencyType currencyType);
    Currency getByCode(CurrencyType currencyType);
}
