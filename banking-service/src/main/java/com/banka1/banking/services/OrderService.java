package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.services.implementation.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final AuthService authService;
    private final AccountService accountService;
    private final BankAccountUtils bankAccountUtils;
    private final AccountRepository accountRepository;

    @Transactional
    public Double executeOrder(String direction, Long userId, Long accountId, Double amount) {
        Account account = accountService.findById(accountId);
        Account bankAccount = bankAccountUtils.getBankAccountForCurrency(account.getCurrencyType());

        Double finalAmount = amount;

        if(Objects.equals(account.getId(), bankAccount.getId())) {
            if(direction.compareToIgnoreCase("buy") == 0) {
                account.setBalance(account.getBalance() - finalAmount);
            } else if(direction.compareToIgnoreCase("sell") == 0) {
                account.setBalance(account.getBalance() + finalAmount);
            } else {
                throw new RuntimeException();
            }

            accountRepository.save(account);
        } else {
            if(!Objects.equals(account.getOwnerID(), userId))
                throw new RuntimeException();

            // TODO: provizija

            if(direction.compareToIgnoreCase("buy") == 0) {
                if(account.getBalance() < finalAmount) {
                    throw new IllegalArgumentException();
                }

                bankAccount.setBalance(bankAccount.getBalance() + finalAmount);
                account.setBalance(account.getBalance() - finalAmount);
            } else if(direction.compareToIgnoreCase("sell") == 0) {
                bankAccount.setBalance(bankAccount.getBalance() - finalAmount);
                account.setBalance(account.getBalance() + finalAmount);
            } else {
                throw new RuntimeException();
            }

            accountRepository.save(account);
            accountRepository.save(bankAccount);
        }

        return finalAmount;
    }
}
