package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountUtils bankAccountUtils;

    @Transactional
    public List<Transaction> getTransactionsByUserId(Long userId) {
        List<Account> accounts = accountRepository.findByOwnerID(userId);
        List<Transaction> transactions = transactionRepository.findByFromAccountIdInOrToAccountIdIn(accounts, accounts);
        if(!Objects.equals(bankAccountUtils.getBankAccountForCurrency(CurrencyType.RSD).getOwnerID(), userId))
            transactions.removeIf(Transaction::getBankOnly);
        return transactions;
    }


    public boolean userExists(Long id){
        return transactionRepository.existsById(id);
    }

    public Transaction findByTransfer(Transfer transfer) {
        return transactionRepository.findByTransferId(transfer.getId()).orElse(null);
    }
}