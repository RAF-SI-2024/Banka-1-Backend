package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;

    private final TransferRepository transferRepository;

    private final CurrencyRepository currencyRepository;

    private final TransactionRepository transactionRepository;

    private final JmsTemplate jmsTemplate;

    private final MessageHelper messageHelper;

    private final String destinationEmail;

    private final UserServiceCustomer userServiceCustomer;

    private final ExchangeService exchangeService;

    private final OtpTokenService otpTokenService;

    private final BankAccountUtils bankAccountUtils;


    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository, TransactionRepository transactionRepository, CurrencyRepository currencyRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, @Value("${destination.email}") String destinationEmail, UserServiceCustomer userServiceCustomer, ExchangeService exchangeService, OtpTokenService otpTokenService, BankAccountUtils bankAccountUtils) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.currencyRepository = currencyRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.destinationEmail = destinationEmail;
        this.userServiceCustomer = userServiceCustomer;
        this.exchangeService = exchangeService;
        this.otpTokenService = otpTokenService;
        this.bankAccountUtils = bankAccountUtils;
    }

    @Transactional
    public String processTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));

        System.out.println("Transfer type: " + transfer.getType());
        return switch (transfer.getType()) {
            case INTERNAL, EXCHANGE -> processInternalTransfer(transferId);
            case EXTERNAL, FOREIGN -> processExternalTransfer(transferId);
            default -> throw new RuntimeException("Invalid transfer type");
        };
    }

    // To be called after the initial amount is stripped from the account initiating the transfer
    // Completes a transfer between two accounts of differing currency types
    @Transactional
    protected Map<String, Object> performCurrencyExchangeTransfer(Transfer transfer, Account fromAccount, Account toAccount) {
        Account toBankAccount = bankAccountUtils.getBankAccountForCurrency(fromAccount.getCurrencyType());
        Account fromBankAccount = bankAccountUtils.getBankAccountForCurrency(toAccount.getCurrencyType());

        toBankAccount.setBalance(toBankAccount.getBalance() + transfer.getAmount());

        Map<String, Object> exchange = exchangeService.calculatePreviewExchangeAutomatic(
                transfer.getFromCurrency().getCode().toString(),
                transfer.getToCurrency().getCode().toString(),
                transfer.getAmount()
        );

        Double exchangedAmount = (Double) exchange.get("finalAmount");

        fromBankAccount.setBalance(fromBankAccount.getBalance() - exchangedAmount);
        toAccount.setBalance(toAccount.getBalance() + exchangedAmount);

        Currency fromCurrency = currencyRepository.getByCode(fromAccount.getCurrencyType());
        Currency toCurrency = currencyRepository.getByCode(toAccount.getCurrencyType());

        Transfer transferToBank = new Transfer();
        transferToBank.setFromAccountId(fromAccount);
        transferToBank.setToAccountId(toBankAccount);
        transferToBank.setAmount(transfer.getAmount());
        transferToBank.setStatus(TransferStatus.COMPLETED);
        transferToBank.setType(TransferType.EXTERNAL);
        transferToBank.setPaymentDescription("Promena valute");
        transferToBank.setReceiver(toBankAccount.getCompany().getName());
        transferToBank.setFromCurrency(fromCurrency);
        transferToBank.setToCurrency(fromCurrency);
        transferToBank.setCreatedAt(System.currentTimeMillis());

        CustomerDTO receiver = userServiceCustomer.getCustomerById(toAccount.getOwnerID());

        Transfer transferFromBank = new Transfer();
        transferFromBank.setFromAccountId(fromBankAccount);
        transferFromBank.setToAccountId(toAccount);
        transferFromBank.setAmount(exchangedAmount);
        transferFromBank.setStatus(TransferStatus.COMPLETED);
        transferFromBank.setType(TransferType.EXTERNAL);
        transferFromBank.setPaymentDescription("Promena valute");
        transferFromBank.setReceiver(receiver.getFirstName() + " " + receiver.getLastName());
        transferFromBank.setFromCurrency(toCurrency);
        transferFromBank.setToCurrency(toCurrency);
        transferFromBank.setCreatedAt(System.currentTimeMillis());

        Transaction transactionToBank = new Transaction();
        transactionToBank.setBankOnly(true);
        transactionToBank.setFromAccountId(fromAccount);
        transactionToBank.setToAccountId(toBankAccount);
        transactionToBank.setAmount(transfer.getAmount());
        transactionToBank.setCurrency(fromCurrency);
        transactionToBank.setFinalAmount(transfer.getAmount());
        transactionToBank.setFee(0.0);
        transactionToBank.setTimestamp(System.currentTimeMillis());
        transactionToBank.setDescription("Exchange transaction");
        transactionToBank.setTransfer(transferToBank);

        Transaction transactionFromBank = new Transaction();
        transactionFromBank.setBankOnly(true);
        transactionFromBank.setFromAccountId(fromBankAccount);
        transactionFromBank.setToAccountId(toAccount);
        transactionFromBank.setAmount(exchangedAmount);
        transactionFromBank.setCurrency(toCurrency);
        transactionFromBank.setFinalAmount(exchangedAmount);
        transactionFromBank.setFee(0.0);
        transactionFromBank.setTimestamp(System.currentTimeMillis());
        transactionFromBank.setDescription("Exchange transaction");
        transactionFromBank.setTransfer(transferFromBank);

        transferRepository.save(transferToBank);
        transferRepository.save(transferFromBank);

        transactionRepository.save(transactionToBank);
        transactionRepository.save(transactionFromBank);

        accountRepository.save(fromBankAccount);
        accountRepository.save(toBankAccount);

        return exchange;
    }

    @Transactional
    public String processInternalTransfer(Long transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> new RuntimeException("Transfer not found"));

        // Provera statusa i tipa transfera
        if (!transfer.getStatus().equals(TransferStatus.PENDING)) {
            throw new RuntimeException("Transfer is not in pending state");
        }

        if (!transfer.getType().equals(TransferType.INTERNAL) && !transfer.getType().equals(TransferType.EXCHANGE)) {
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
            Map<String, Object> exchangeMap = null;

            if(transfer.getType().equals(TransferType.INTERNAL))
                toAccount.setBalance(toAccount.getBalance() + transfer.getAmount());
            else {
                exchangeMap = performCurrencyExchangeTransfer(transfer, fromAccount, toAccount);
            }

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Kreiranje transakcija
            Transaction debitTransaction = new Transaction();
            debitTransaction.setFromAccountId(fromAccount);
            debitTransaction.setToAccountId(toAccount);
            debitTransaction.setAmount(transfer.getAmount());
            debitTransaction.setCurrency(transfer.getFromCurrency());
            if(exchangeMap != null) {
                debitTransaction.setFee((Double) exchangeMap.get("fee"));
                debitTransaction.setFinalAmount((Double) exchangeMap.get("finalAmount"));
            } else {
                debitTransaction.setFee(0.0);
                debitTransaction.setFinalAmount(transfer.getAmount());
            }
            debitTransaction.setTimestamp(System.currentTimeMillis());
            debitTransaction.setDescription("Debit transaction for transfer " + transferId);
            debitTransaction.setTransfer(transfer);

            transactionRepository.save(debitTransaction);

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

        if (!transfer.getType().equals(TransferType.EXTERNAL) && !transfer.getType().equals(TransferType.FOREIGN)) {
            throw new RuntimeException("Invalid transfer type for this process");
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
            Map<String, Object> exchangeMap = null;

            if(transfer.getType().equals(TransferType.EXTERNAL))
                toAccount.setBalance(toAccount.getBalance() + transfer.getAmount());
            else {
                exchangeMap = performCurrencyExchangeTransfer(transfer, fromAccount, toAccount);
            }

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            Transaction debitTransaction = new Transaction();
            debitTransaction.setFromAccountId(fromAccount);
            debitTransaction.setToAccountId(toAccount);
            debitTransaction.setAmount(amount);
            debitTransaction.setCurrency(transfer.getFromCurrency());
            if(exchangeMap != null) {
                log.info(exchangeMap.toString());
                debitTransaction.setFee((Double) exchangeMap.get("fee"));
                debitTransaction.setFinalAmount((Double) exchangeMap.get("finalAmount"));
            } else {
                debitTransaction.setFee(0.0);
                debitTransaction.setFinalAmount(transfer.getAmount());
            }
            debitTransaction.setTimestamp(Instant.now().toEpochMilli());
            debitTransaction.setDescription("Debit transaction for transfer " + transfer.getId());
            debitTransaction.setTransfer(transfer);
            transactionRepository.save(debitTransaction);

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(Instant.now().toEpochMilli());
            transferRepository.save(transfer);

            return "Transfer completed successfully";
        } catch (Exception e) {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setNote("Error during processing: " + e.getMessage());
            log.error(e.getMessage());
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

    public Transfer createInternalTransferEntity(Account fromAccount, Account toAccount, double amount, CustomerDTO customerData, String description) {
        Currency fromCurrency = currencyRepository.findByCode(fromAccount.getCurrencyType())
                .orElseThrow(() -> new IllegalArgumentException("Greska"));

        Currency toCurrency = currencyRepository.findByCode(toAccount.getCurrencyType())
                .orElseThrow(() -> new IllegalArgumentException("Greska"));

        String firstName = customerData.getFirstName();
        String lastName = customerData.getLastName();

        Transfer transfer = new Transfer();
        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(amount);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setType(TransferType.INTERNAL);
        transfer.setPaymentDescription(description);
        transfer.setReceiver(firstName + " " + lastName);
        transfer.setFromCurrency(fromCurrency);
        transfer.setToCurrency(toCurrency);
        transfer.setCreatedAt(System.currentTimeMillis());

        return transferRepository.saveAndFlush(transfer);
    }

    public Long createInternalTransfer(InternalTransferDTO internalTransferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findById(internalTransferDTO.getFromAccountId());
        Optional<Account> toAccountOtp = accountRepository.findById(internalTransferDTO.getToAccountId());

        if (fromAccountOtp.isPresent() && toAccountOtp.isPresent()){

            Account fromAccount = fromAccountOtp.get();
            Account toAccount = toAccountOtp.get();

            Long customerId = fromAccount.getOwnerID();
            CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

            if (customerData == null ) {
                throw new IllegalArgumentException("Korisnik nije pronađen");
            }

            String email = customerData.getEmail();
            String firstName = customerData.getFirstName();
            String lastName = customerData.getLastName();

            var transfer = createInternalTransferEntity(fromAccount, toAccount, internalTransferDTO.getAmount(), customerData, "Interni prenos");

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

            NotificationDTO pushNotification = new NotificationDTO();
            pushNotification.setSubject("Verifikacija");
            pushNotification.setMessage("Kliknite kako biste verifikovali transfer");
            pushNotification.setFirstName(firstName);
            pushNotification.setLastName(lastName);
            pushNotification.setType("firebase");
            pushNotification.setEmail(email);
            Map<String, String> data = Map.of("transferId", transfer.getId().toString(), "otp", otpCode);
            pushNotification.setAdditionalData(data);

            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(emailDto));
            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(pushNotification));

            return transfer.getId();
        }
        return null;
    }

    public Transfer createMoneyTransferEntity(Account fromAccount, Account toAccount, MoneyTransferDTO moneyTransferDTO) {
        Currency fromCurrency = currencyRepository.findByCode(fromAccount.getCurrencyType())
                .orElseThrow(() -> new IllegalArgumentException("Greska"));

        Currency toCurrency = currencyRepository.findByCode(toAccount.getCurrencyType())
                .orElseThrow(() -> new IllegalArgumentException("Greska"));

        Long customerId = fromAccount.getOwnerID();
        CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

        if (customerData == null ) {
            throw new IllegalArgumentException("Korisnik nije pronađen");
        }

        Transfer transfer = new Transfer();
        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(moneyTransferDTO.getAmount());
        transfer.setReceiver(moneyTransferDTO.getReceiver());
        transfer.setAdress(moneyTransferDTO.getAdress() != null ? moneyTransferDTO.getAdress() : "N/A");
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setType(fromCurrency.equals(toCurrency) ? TransferType.EXTERNAL : TransferType.FOREIGN);
        transfer.setFromCurrency(fromCurrency);
        transfer.setToCurrency(toCurrency);
        transfer.setPaymentCode(moneyTransferDTO.getPayementCode());
        transfer.setPaymentReference(moneyTransferDTO.getPayementReference() != null ? moneyTransferDTO.getPayementReference() : "N/A");
        transfer.setPaymentDescription(moneyTransferDTO.getPayementDescription());
        transfer.setCreatedAt(System.currentTimeMillis());

        return transferRepository.saveAndFlush(transfer);
    }

    public Long createMoneyTransfer(MoneyTransferDTO moneyTransferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findByAccountNumber(moneyTransferDTO.getFromAccountNumber());
        Optional<Account> toAccountOtp = accountRepository.findByAccountNumber(moneyTransferDTO.getRecipientAccount());

        if (fromAccountOtp.isPresent() && toAccountOtp.isPresent()){

            Account fromAccount = fromAccountOtp.get();
            Account toAccount = toAccountOtp.get();

            Long customerId = fromAccount.getOwnerID();
            CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

            if (customerData == null ) {
                throw new IllegalArgumentException("Korisnik nije pronađen");
            }

            String email = customerData.getEmail();
            String firstName = customerData.getFirstName();
            String lastName = customerData.getLastName();

            var transfer = createMoneyTransferEntity(fromAccount, toAccount, moneyTransferDTO);

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

            NotificationDTO pushNotification = new NotificationDTO();
            pushNotification.setSubject("Verifikacija");
            pushNotification.setMessage("Kliknite kako biste verifikovali transfer");
            pushNotification.setFirstName(firstName);
            pushNotification.setLastName(lastName);
            pushNotification.setEmail(email);
            pushNotification.setType("firebase");
            Map<String, String> data = Map.of("transferId", transfer.getId().toString(), "otp", otpCode);
            pushNotification.setAdditionalData(data);

            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(emailDto));
            jmsTemplate.convertAndSend(destinationEmail,messageHelper.createTextMessage(pushNotification));

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

