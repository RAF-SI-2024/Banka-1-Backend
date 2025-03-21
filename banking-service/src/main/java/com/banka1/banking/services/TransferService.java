package com.banka1.banking.services;

import com.banka1.banking.dto.*;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.repository.TransferRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;

    private final TransferRepository transferRepository;

    private final CurrencyRepository currencyRepository;

    private final TransactionRepository transactionRepository;

    private final JmsTemplate jmsTemplate;

    private final MessageHelper messageHelper;

    private final String destinationEmail;

    private final UserServiceCustomer userServiceCustomer;

    private final OtpTokenService otpTokenService;


    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository, TransactionRepository transactionRepository, CurrencyRepository currencyRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, @Value("${destination.email}") String destinationEmail, UserServiceCustomer userServiceCustomer, OtpTokenService otpTokenService) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.destinationEmail = destinationEmail;
        this.userServiceCustomer = userServiceCustomer;
        this.otpTokenService = otpTokenService;
    }

    @Transactional
    public String processTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        System.out.println("Transfer type: " + transfer.getType());
        switch (transfer.getType()) {
            case INTERNAL:
                return processInternalTransfer(transferId);
            case EXTERNAL: case FOREIGN:
                return processExternalTransfer(transferId);
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

        try{
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
        }catch (Exception e) {
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

    public boolean validateInternalTransfer(InternalTransferDTO transferDTO){

        Optional<Account> fromAccountInternal = accountRepository.findById(transferDTO.getFromAccountId());
        Optional<Account> toAccountInternal = accountRepository.findById(transferDTO.getToAccountId());

        if(fromAccountInternal.isEmpty() || toAccountInternal.isEmpty()){
            return false;
        }

        Account fromAccount = fromAccountInternal.get();
        Account toAccount = toAccountInternal.get();

        if(!fromAccount.getCurrencyType().equals(toAccount.getCurrencyType())){
            return false;
        }

        return fromAccount.getOwnerID().equals(toAccount.getOwnerID());
    }

    public boolean validateMoneyTransfer(MoneyTransferDTO transferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findByAccountNumber(transferDTO.getFromAccountNumber());
        Optional<Account> toAccountOtp = accountRepository.findByAccountNumber(transferDTO.getRecipientAccount());

        if(fromAccountOtp.isEmpty() || toAccountOtp.isEmpty()){
            return false;
        }

        Account fromAccount = fromAccountOtp.get();
        Account toAccount = toAccountOtp.get();

        return !fromAccount.getOwnerID().equals(toAccount.getOwnerID());
    }


    public Long createInternalTransfer(InternalTransferDTO internalTransferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findById(internalTransferDTO.getFromAccountId());
        Optional<Account> toAccountOtp = accountRepository.findById(internalTransferDTO.getToAccountId());

        if (fromAccountOtp.isPresent() && toAccountOtp.isPresent()){

            Account fromAccount = fromAccountOtp.get();
            Account toAccount = toAccountOtp.get();

            Currency fromCurrency = currencyRepository.findByCode(fromAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Currency toCurrency = currencyRepository.findByCode(toAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Long customerId = fromAccount.getOwnerID();
            CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

            if (customerData == null ) {
                throw new IllegalArgumentException("Korisnik nije pronađen");
            }

            String email = customerData.getEmail();
            String firstName = customerData.getFirstName();
            String lastName = customerData.getLastName();

            Transfer transfer = new Transfer();
            transfer.setFromAccountId(fromAccount);
            transfer.setToAccountId(toAccount);
            transfer.setAmount(internalTransferDTO.getAmount());
            transfer.setStatus(TransferStatus.PENDING);
            transfer.setType(TransferType.INTERNAL);
            transfer.setPaymentDescription("Interni prenos");
            transfer.setReceiver(firstName + " " + lastName);
            transfer.setFromCurrency(fromCurrency);
            transfer.setToCurrency(toCurrency);
            transfer.setCreatedAt(System.currentTimeMillis());

            transferRepository.saveAndFlush(transfer);

            String otpCode = otpTokenService.generateOtp(transfer.getId());
            transfer.setOtp(otpCode);
            transferRepository.save(transfer);

            NotificationDTO emailDto = new NotificationDTO();
            emailDto.setSubject("Verifikacija");
            emailDto.setEmail(email);
            emailDto.setMessage("Vaš verifikacioni kod je: " + otpCode);
            emailDto.setFirstName(firstName);
            emailDto.setLastName(lastName);
            emailDto.setType("email");

            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(emailDto));
            return transfer.getId();
        }
        return null;
    }

    public Long createMoneyTransfer(MoneyTransferDTO moneyTransferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findByAccountNumber(moneyTransferDTO.getFromAccountNumber());
        Optional<Account> toAccountOtp = accountRepository.findByAccountNumber(moneyTransferDTO.getRecipientAccount());

        if (fromAccountOtp.isPresent() && toAccountOtp.isPresent()){

            Account fromAccount = fromAccountOtp.get();
            Account toAccount = toAccountOtp.get();

            Currency fromCurrency = currencyRepository.findByCode(fromAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Currency toCurrency = currencyRepository.findByCode(toAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Long customerId = fromAccount.getOwnerID();
            CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

            if (customerData == null ) {
                throw new IllegalArgumentException("Korisnik nije pronađen");
            }

            String email = customerData.getEmail();
            String firstName = customerData.getFirstName();
            String lastName = customerData.getLastName();

            Transfer transfer = new Transfer();
            transfer.setFromAccountId(fromAccount);
            transfer.setToAccountId(toAccount);
            transfer.setAmount(moneyTransferDTO.getAmount());
            transfer.setReceiver(moneyTransferDTO.getReceiver());
            transfer.setAdress(moneyTransferDTO.getAdress() != null ? moneyTransferDTO.getAdress() : "N/A");
            transfer.setStatus(TransferStatus.PENDING);
            transfer.setType(fromCurrency.equals(toCurrency) ? TransferType.FOREIGN : TransferType.EXTERNAL);
            transfer.setFromCurrency(fromCurrency);
            transfer.setToCurrency(toCurrency);
            transfer.setPaymentCode(moneyTransferDTO.getPayementCode());
            transfer.setPaymentReference(moneyTransferDTO.getPayementReference() != null ? moneyTransferDTO.getPayementReference() : "N/A");
            transfer.setPaymentDescription(moneyTransferDTO.getPayementDescription());
            transfer.setCreatedAt(System.currentTimeMillis());

            transferRepository.saveAndFlush(transfer);

            String otpCode = otpTokenService.generateOtp(transfer.getId());
            transfer.setOtp(otpCode);
            transferRepository.save(transfer);

            NotificationDTO emailDto = new NotificationDTO();
            emailDto.setSubject("Verifikacija");
            emailDto.setEmail(email);
            emailDto.setMessage("Vaš verifikacioni kod je: " + otpCode);
            emailDto.setFirstName(firstName);
            emailDto.setLastName(lastName);
            emailDto.setType("email");

            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(emailDto));
            return transfer.getId();

        }
        return null;
    }

    @Scheduled(fixedRate = 10000)
    public void cancelExpiredTransfers(){

        long expirationTime = System.currentTimeMillis() - (5*60*1000);

        List<Transfer> expiredTransfers = transferRepository.findAllByStatusAndCreatedAtBefore(TransferStatus.PENDING,expirationTime);

        for (Transfer transfer : expiredTransfers){
            transfer.setStatus(TransferStatus.CANCELLED);
            transferRepository.save(transfer);
        }

    }

    public Transfer findById(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer sa ID-jem " + transferId + " nije pronađen"));
    }
}

