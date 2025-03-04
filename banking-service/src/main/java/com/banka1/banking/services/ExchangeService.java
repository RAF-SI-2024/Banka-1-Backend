package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransferRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ExchangeService {

    private final AccountRepository accountRepository;

    private final CurrencyRepository currencyRepository;

    private final TransferRepository transferRepository;

    private final JmsTemplate jmsTemplate;

    private final MessageHelper messageHelper;

    private final String destinationEmail;

    private final UserServiceCustomer userServiceCustomer;

    private final OtpTokenService otpTokenService;

    public ExchangeService(AccountRepository accountRepository, CurrencyRepository currencyRepository, TransferRepository transferRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, @Value("send-email") String destinationEmail, UserServiceCustomer userServiceCustomer, OtpTokenService otpTokenService) {
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.transferRepository = transferRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.destinationEmail = destinationEmail;
        this.userServiceCustomer = userServiceCustomer;
        this.otpTokenService = otpTokenService;
    }

    public boolean validateExchangeTransfer(ExchangeMoneyTransferDTO exchangeMoneyTransferDTO){

        Optional<Account> fromAccountOtp = accountRepository.findById(exchangeMoneyTransferDTO.getAccountFrom());
        Optional<Account> toAccountOtp = accountRepository.findById(exchangeMoneyTransferDTO.getAccountTo());

        if (fromAccountOtp.isEmpty() || toAccountOtp.isEmpty()){
            return false;
        }

        Account fromAccount = fromAccountOtp.get();
        Account toAccount = toAccountOtp.get();

        if (fromAccount.getCurrencyType().equals(toAccount.getCurrencyType())){
            return false;
        }

        if (!fromAccount.getOwnerID().equals(toAccount.getOwnerID())){
            return false;
        }

        return true;
    }

    public Long createExchangeTransfer(ExchangeMoneyTransferDTO exchangeMoneyTransferDTO) {

        Optional<Account> fromAccountDTO = accountRepository.findById(exchangeMoneyTransferDTO.getAccountFrom());
        Optional<Account> toAccountDTO = accountRepository.findById(exchangeMoneyTransferDTO.getAccountTo());

        if (fromAccountDTO.isPresent() && toAccountDTO.isPresent()) {

            Account fromAccount = fromAccountDTO.get();
            Account toAccount = toAccountDTO.get();

            // PROVERITI DA LI SE VALUTE SALJU U DTO
            Currency fromCurrency = currencyRepository.findByCode(fromAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Currency toCurrency = currencyRepository.findByCode(toAccount.getCurrencyType())
                    .orElseThrow(() -> new IllegalArgumentException("Greska"));

            Long customerId = fromAccount.getOwnerID();
            CustomerDTO customerData = userServiceCustomer.getCustomerById(customerId);

            if (customerData == null) {
                throw new IllegalArgumentException("Korisnik nije pronađen");
            }

            String email = customerData.getEmail();
            String firstName = customerData.getFirstName();
            String lastName = customerData.getLastName();

            Transfer transfer = new Transfer();
            transfer.setFromAccountId(fromAccount);
            transfer.setToAccountId(toAccount);
            transfer.setAmount(exchangeMoneyTransferDTO.getAmount());
            transfer.setStatus(TransferStatus.PENDING);
            transfer.setType(TransferType.EXCHANGE);
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

            jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDto));
            return transfer.getId();
        }

        return null;
    }

}
