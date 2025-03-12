package com.banka1.banking.repository;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.helper.CurrencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
     List<Account> findByOwnerID(Long ownerId);
    Optional<Account> findById(Long accountId);
    boolean existsByOwnerID(Long checkId);
    boolean existsByAccountNumber(@NotBlank @Size(min = 2, max = 30) String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
    Account findByOwnerIDAndCurrencyType(Long ownerId, CurrencyType currencyType);
}
