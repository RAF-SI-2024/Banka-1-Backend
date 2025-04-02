package com.banka1.banking.repository;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountId(Account fromAccountId);
    List<Transaction> findByToAccountId(Account toAccountId);
    Optional<Transaction> findByTransferId(Long transferId);
    List<Transaction> findByFromAccountIdInOrToAccountIdIn(List<Account> fromAccounts, List<Account> toAccounts);
}
