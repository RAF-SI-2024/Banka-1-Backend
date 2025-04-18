package com.banka1.banking.services;

import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.OrderTransactionInitiationDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.services.implementation.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banka1.banking.repository.TransactionRepository;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final AccountService accountService;
    private final BankAccountUtils bankAccountUtils;
    private final AccountRepository accountRepository;
    private final TransferService transferService;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final CurrencyRepository currencyRepository;

    @Transactional
    public Double executeOrder(String direction, Long userId, Long accountId, Double amount, Double fee) {
        Account account = accountService.findById(accountId);
        Account bankAccount = bankAccountUtils.getBankAccountForCurrency(account.getCurrencyType());

        if (!Objects.equals(account.getOwnerID(), userId)) {
            throw new RuntimeException("Korisnik nije vlasnik računa");
        }

        if (direction.equalsIgnoreCase("buy") && account.getBalance() < amount + (fee != null ? fee : 0)) {
            throw new IllegalArgumentException("Nedovoljno sredstava na računu za iznos + proviziju");
        }

        boolean sameAccount = Objects.equals(account.getId(), bankAccount.getId());

        if (sameAccount) {
            if (direction.equalsIgnoreCase("buy")) {
                account.setBalance(account.getBalance() - amount - (fee != null ? fee : 0));
            } else if (direction.equalsIgnoreCase("sell")) {
                account.setBalance(account.getBalance() + amount);
            } else {
                throw new IllegalArgumentException("Nepoznata direkcija");
            }
            accountRepository.save(account);

        } else {
            if (direction.equalsIgnoreCase("buy")) {
                // Napravi transfer za kupovinu
                MoneyTransferDTO transferDto = new MoneyTransferDTO();
                transferDto.setFromAccountNumber(account.getAccountNumber());
                transferDto.setRecipientAccount(bankAccount.getAccountNumber());
                transferDto.setAmount(amount);
                transferDto.setReceiver("Order Execution");
                transferDto.setAdress("System");
                transferDto.setPayementCode("999");
                transferDto.setPayementReference("Auto");
                transferDto.setPayementDescription("Realizacija kupovine hartije");

                transferService.createMoneyTransfer(transferDto);

                // Napravi dodatni transfer za fee ako postoji
                if (fee != null && fee > 0) {
                    MoneyTransferDTO feeTransferDto = new MoneyTransferDTO();
                    feeTransferDto.setFromAccountNumber(account.getAccountNumber());
                    feeTransferDto.setRecipientAccount(bankAccount.getAccountNumber());
                    feeTransferDto.setAmount(fee);
                    feeTransferDto.setReceiver("Bank Fee");
                    feeTransferDto.setAdress("System");
                    feeTransferDto.setPayementCode("999");
                    feeTransferDto.setPayementReference("Fee");
                    feeTransferDto.setPayementDescription("Provizija za realizaciju naloga");

                    transferService.createMoneyTransfer(feeTransferDto);
                }
            } else if (direction.equalsIgnoreCase("sell")) {
                // Za prodaju: novac ide od banke ka korisniku
                MoneyTransferDTO transferDto = new MoneyTransferDTO();
                transferDto.setFromAccountNumber(bankAccount.getAccountNumber());
                transferDto.setRecipientAccount(account.getAccountNumber());
                transferDto.setAmount(amount);
                transferDto.setReceiver("Order Execution - Sell");
                transferDto.setAdress("System");
                transferDto.setPayementCode("999");
                transferDto.setPayementReference("Auto");
                transferDto.setPayementDescription("Realizacija prodaje hartije");

                transferService.createMoneyTransfer(transferDto);
            } else {
                throw new IllegalArgumentException("Nepoznata direkcija");
            }
        }

        return amount;
    }

    @Transactional
    public void processOrderTransaction(OrderTransactionInitiationDTO dto) {
        System.out.println("=== POČINJE processOrderTransaction ===");
        System.out.println("Buyer ID: " + dto.getBuyerAccountId());
        System.out.println("Seller ID: " + dto.getSellerAccountId());
        System.out.println("Amount: " + dto.getAmount());

        Account buyer = accountRepository.findById(dto.getBuyerAccountId()).orElseThrow();
        Account seller = accountRepository.findById(dto.getSellerAccountId()).orElseThrow();

        System.out.println("Buyer Account found: " + buyer.getAccountNumber());
        System.out.println("Seller Account found: " + seller.getAccountNumber());

        if (!buyer.getCurrencyType().equals(seller.getCurrencyType())) {
            System.out.println("Greska: Currency mismatch!");
            throw new IllegalArgumentException("Currency mismatch");
        }

        if (buyer.getBalance() < dto.getAmount()) {
            System.out.println("Greska: Insufficient funds!");
            throw new IllegalArgumentException("Insufficient funds");
        }

        System.out.println("Pre transfera - Buyer balance: " + buyer.getBalance());
        System.out.println("Pre transfera - Seller balance: " + seller.getBalance());

        buyer.setBalance(buyer.getBalance() - dto.getAmount());
        seller.setBalance(seller.getBalance() + dto.getAmount());

        accountRepository.save(buyer);
        accountRepository.save(seller);

        System.out.println("Posle transfera - Buyer balance: " + buyer.getBalance());
        System.out.println("Posle transfera - Seller balance: " + seller.getBalance());

        MoneyTransferDTO moneyTransferDTO = new MoneyTransferDTO();
        moneyTransferDTO.setFromAccountNumber(buyer.getAccountNumber());
        moneyTransferDTO.setRecipientAccount(seller.getAccountNumber());
        moneyTransferDTO.setAmount(dto.getAmount());
        moneyTransferDTO.setReceiver("Order Execution Transfer");
        moneyTransferDTO.setAdress("N/A");
        moneyTransferDTO.setPayementCode("999");
        moneyTransferDTO.setPayementReference("Auto");
        moneyTransferDTO.setPayementDescription("Transfer initiated via Orders");

        System.out.println("Kreiram transfer entity...");
        Transfer transfer = transferService.createMoneyTransferEntity(buyer, seller, moneyTransferDTO);
        transfer.setStatus(TransferStatus.COMPLETED);

        System.out.println("Transfer kreiran. ID: " + transfer.getId());

        System.out.println("Kreiram transakciju...");
        Transaction transaction = new Transaction();
        transaction.setAmount(dto.getAmount());
        transaction.setFinalAmount(dto.getAmount());
        transaction.setFee(0.0);
        transaction.setCurrency(currencyRepository.getByCode(buyer.getCurrencyType()));
        transaction.setDescription("Order Execution Transfer");
        transaction.setTimestamp(Instant.now().toEpochMilli());
        transaction.setFromAccountId(buyer);
        transaction.setToAccountId(seller);
        transaction.setTransfer(transfer);
        transaction.setBankOnly(false);

        transactionRepository.save(transaction);

        System.out.println("Transakcija sacuvana! ID transakcije: " + transaction.getId());
        System.out.println("=== ZAVRŠEN processOrderTransaction ===");
    }


}
