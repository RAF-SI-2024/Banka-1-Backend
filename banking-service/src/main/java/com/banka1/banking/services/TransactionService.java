package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


//  MISLIM DA SVE VEZANO ZA TRANSAKCIJE TREBA PREBACITI U OVAJ SERVIS
// OSTAVIO SAM DA SE OBRADA TRANSAKCIJA POZIVA IZ TRANSFER SERVISA SADA ZBOG TESTOVA ALI PROMENITI ZA SLEDECI SPRINT
// TREBA DODATI OBRADU DEVIZNOG PLACANJA I MENJACNICE

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional
    public String processTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        switch (transfer.getType()) {
            case INTERNAL:
                return processInternalTransfer(transferId);
            case EXTERNAL:
                return processExternalTransfer(transferId);
            case FOREIGN:
                return null;
            case EXCHANGE:
                throw new RuntimeException("Exchange transfer not implemented");
            default:
                throw new RuntimeException("Invalid transfer type");
        }
    }


    @Transactional
    public String processInternalTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> new RuntimeException("Transfer not found"));

        // Provera statusa i tipa transfera
        if (!transfer.getStatus().equals(TransferStatus.PENDING)) {
            throw new RuntimeException("Transfer is not in pending state");
        }

        if (!transfer.getType().equals(TransferType.INTERNAL)) {
            throw new RuntimeException("Invalid transfer type for this process");
        }

        Account fromAccount = transfer.getFromAccountId();
        Account toAccount = transfer.getToAccountId();

        //Ukoliko na racunu ne postoji dovoljno sredstava za izvrsenje
        if (fromAccount.getBalance() < transfer.getAmount()) {
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            throw new RuntimeException("Insufficient funds");
        }

        try {
            // Azuriranje balansa
            fromAccount.setBalance(fromAccount.getBalance() - transfer.getAmount());
            toAccount.setBalance(toAccount.getBalance() + transfer.getAmount());
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Kreiranje transakcija
            Transaction debitTransaction = new Transaction();
            debitTransaction.setFromAccountId(fromAccount);
            debitTransaction.setToAccountId(toAccount);
            debitTransaction.setAmount(transfer.getAmount());
            debitTransaction.setCurrency(transfer.getFromCurrency());
            debitTransaction.setTimestamp(System.currentTimeMillis());
            debitTransaction.setDescription("Debit transaction for transfer " + transferId);
            debitTransaction.setTransfer(transfer);

            Transaction creditTransaction = new Transaction();
            creditTransaction.setFromAccountId(fromAccount);
            creditTransaction.setToAccountId(toAccount);
            creditTransaction.setAmount(transfer.getAmount());
            creditTransaction.setCurrency(transfer.getToCurrency());
            creditTransaction.setTimestamp(System.currentTimeMillis());
            creditTransaction.setDescription("Credit transaction for transfer " + transferId);
            creditTransaction.setTransfer(transfer);

            transactionRepository.save(debitTransaction);
            transactionRepository.save(creditTransaction);

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(System.currentTimeMillis());
            transferRepository.save(transfer);

            return "Transfer completed successfully";
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed, rollback initiated", e);
        }

    }

    @Transactional
    public String processExternalTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        if (!transfer.getStatus().equals(TransferStatus.PENDING)) {
            throw new RuntimeException("Transfer is not in pending state");
        }

        Account fromAccount = transfer.getFromAccountId();
        Account toAccount = transfer.getToAccountId();
        Double amount = transfer.getAmount();

        if (fromAccount.getBalance() < amount) {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setNote("Insufficient balance");
            transferRepository.save(transfer);
            throw new RuntimeException("Insufficient balance for transfer");
        }

        try {
            fromAccount.setBalance(fromAccount.getBalance() - amount);
            accountRepository.save(fromAccount);

            toAccount.setBalance(toAccount.getBalance() + amount);
            accountRepository.save(toAccount);

            Transaction debitTransaction = new Transaction();
            debitTransaction.setFromAccountId(fromAccount);
            debitTransaction.setToAccountId(toAccount);
            debitTransaction.setAmount(amount);
            debitTransaction.setCurrency(transfer.getFromCurrency());
            debitTransaction.setTimestamp(Instant.now().toEpochMilli());
            debitTransaction.setDescription("Debit transaction for transfer " + transfer.getId());
            debitTransaction.setTransfer(transfer);
            transactionRepository.save(debitTransaction);

            Transaction creditTransaction = new Transaction();
            creditTransaction.setFromAccountId(fromAccount);
            creditTransaction.setToAccountId(toAccount);
            creditTransaction.setAmount(amount);
            creditTransaction.setCurrency(transfer.getToCurrency());
            creditTransaction.setTimestamp(Instant.now().toEpochMilli());
            creditTransaction.setDescription("Credit transaction for transfer " + transfer.getId());
            creditTransaction.setTransfer(transfer);
            transactionRepository.save(creditTransaction);

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(Instant.now().toEpochMilli());
            transferRepository.save(transfer);

            return "Transfer completed successfully";
        } catch (Exception e) {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setNote("Error during processing: " + e.getMessage());
            transferRepository.save(transfer);
            throw new RuntimeException("Transfer processing failed", e);
        }
    }

    @Transactional
    public List<Transaction> getTransactionsByUserId(Long userId) {
        List<Account> accounts = accountRepository.findByOwnerID(userId);
        List<Transaction> transactions = transactionRepository.findByFromAccountIdIn(accounts);
        return transactions;
    }

    public Boolean processInstallment(Account customerAccount, Account bankAccount, Installment installment) {
        Double amount = installment.getAmount();
            if (customerAccount.getBalance().compareTo(amount) >= 0) {
//                customerAccount.withdraw(amount);
//                bankAccount.deposit(amount);
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(customerAccount);
                transaction.setToAccountId(bankAccount);
                transaction.setAmount(amount);
                transaction.setCurrency(currencyRepository.getByCode(customerAccount.getCurrencyType()));
                transaction.setTimestamp(Instant.now().getEpochSecond());
                transaction.setDescription("Installment for loan " + installment.getLoan());
                transaction.setLoanId(installment.getLoan().getId());

                transactionRepository.save(transaction);
                accountRepository.save(customerAccount);
                accountRepository.save(bankAccount);
                return true;
            }
        return false;
    }
}