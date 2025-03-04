package com.banka1.banking.services;

import com.banka1.banking.dto.*;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransferRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransferService {


    private final AccountRepository accountRepository;

    private final TransferRepository transferRepository;

    private final CurrencyRepository currencyRepository;

    private final JmsTemplate jmsTemplate;

    private final MessageHelper messageHelper;

    private final String destinationEmail;

    private final UserServiceCustomer userServiceCustomer;

    private final OtpTokenService otpTokenService;


    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository, CurrencyRepository currencyRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, @Value("${destination.email}") String destinationEmail, UserServiceCustomer userServiceCustomer, OtpTokenService otpTokenService) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.currencyRepository = currencyRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.destinationEmail = destinationEmail;
        this.userServiceCustomer = userServiceCustomer;
        this.otpTokenService = otpTokenService;
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

        Optional<Account> fromAccountOtp = accountRepository.findById(transferDTO.getFromAccountId());
        Optional<Account> toAccountOtp = accountRepository.findById(transferDTO.getToAccountId());

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

        Optional<Account> fromAccountOtp = accountRepository.findById(moneyTransferDTO.getFromAccountId());
        Optional<Account> toAccountOtp = accountRepository.findById(moneyTransferDTO.getToAccountId());

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

        long expirationTime = System.currentTimeMillis() - (5*6*1000);

        List<Transfer> expiredTransfers = transferRepository.findAllByStatusAndCreatedAtBefore(TransferStatus.PENDING,expirationTime);

        for (Transfer transfer : expiredTransfers){
            transfer.setStatus(TransferStatus.CANCELLED);
            transferRepository.save(transfer);
        }

    }

}

