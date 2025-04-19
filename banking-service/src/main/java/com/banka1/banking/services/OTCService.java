package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.OTCTransactionACKDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.repository.*;
import com.banka1.banking.models.OTCTransaction;
import com.banka1.common.listener.MessageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
@Slf4j
@RequiredArgsConstructor
public class OTCService {
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    @Value("${destination.otc.ack.trade}")
    private String destinationOtcAck;
    private final OTCTransactionRepository otcTransactionRepository;
    private final AccountRepository accountRepository;
    private final TaskScheduler taskScheduler;
    private final TransferService transferService;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final UserServiceCustomer userServiceCustomer;
    private final CurrencyRepository currencyRepository;

    private void sendFailureMessage(String uid, String message) throws JmsException {
        jmsTemplate.convertAndSend(destinationOtcAck, messageHelper.createTextMessage(new OTCTransactionACKDTO(
                uid, true, message
        )));
    }

    private void retryableFailureMessage(String uid, String message, boolean rollback) {
        try {
            sendFailureMessage(uid, message);
            if(rollback) rollback(uid);
        } catch(JmsException jms) {
            taskScheduler.schedule(() -> retryableFailureMessage(uid, message, rollback), Instant.now().plusSeconds(5));
        }
    }

    private void nextStage(OTCTransaction transaction) {
        nextStage(transaction.getUid(), transaction.getAmountTaken() > 0, transaction.getAmountGiven() > 0, transaction.getFinished());
    }

    private synchronized void nextStage(String uid, boolean assetsTaken, boolean assetsGiven, boolean finished) {
        OTCTransaction transaction = otcTransactionRepository.findByUid(uid).orElse(null);
        if(transaction == null || (transaction.getAmountTaken() > 0 != assetsTaken) || (transaction.getAmountGiven() > 0 != assetsGiven) || (transaction.getFinished() != finished))
            return;

        try {
            jmsTemplate.convertAndSend(destinationOtcAck, messageHelper.createTextMessage(new OTCTransactionACKDTO(
                    transaction.getUid(), false, ""
            )));
        } catch (JmsException jms) {
            taskScheduler.schedule(() -> nextStage(uid, assetsTaken, assetsGiven, finished), Instant.now().plusSeconds(5));
        }
    }

    public synchronized void rollback(String uid) {
        OTCTransaction transaction = otcTransactionRepository.findByUid(uid).orElse(null);
        if(transaction == null)
            return;

        if(transaction.getAmountGiven() > 0)
            transaction.getSellerAccount().setBalance(transaction.getSellerAccount().getBalance() - transaction.getAmountGiven());

        if(transaction.getAmountTaken() > 0)
            transaction.getBuyerAccount().setBalance(transaction.getBuyerAccount().getBalance() + transaction.getAmountTaken());

        accountRepository.save(transaction.getSellerAccount());
        accountRepository.save(transaction.getBuyerAccount());

        otcTransactionRepository.delete(transaction);
        otcTransactionRepository.flush();
    }

    public synchronized void proceed(String uid) {
        OTCTransaction transaction = otcTransactionRepository.findByUid(uid).orElse(null);
        try {
            if(transaction == null)
                throw new Exception("Invalid UID");

            if(transaction.getFailed())
                return;

            if(transaction.getFinished()) {
                log.info("Finishing transaction " + uid);

                Account fromAccount = transaction.getBuyerAccount();
                Account toAccount = transaction.getSellerAccount();

                MoneyTransferDTO dto = new MoneyTransferDTO();
                CustomerDTO customer = userServiceCustomer.getCustomerById(toAccount.getOwnerID());

                dto.setAdress(customer.getAddress());
                dto.setAmount(transaction.getAmount());
                dto.setReceiver(customer.getFirstName() + " " + customer.getLastName());
                dto.setRecipientAccount(toAccount.getAccountNumber());
                dto.setFromAccountNumber(fromAccount.getAccountNumber());
                dto.setPayementDescription("OTC transakcija");
                dto.setPayementCode("519");
                dto.setPayementReference(null);

                Transfer transfer = transferService.createMoneyTransferEntity(
                        fromAccount,
                        toAccount,
                        dto
                );

                transfer.setStatus(TransferStatus.COMPLETED);

                Transaction bankTransaction = new Transaction();

                bankTransaction.setBankOnly(false);
                bankTransaction.setFinalAmount(transaction.getAmount());
                bankTransaction.setFee(0.0);
                bankTransaction.setCurrency(currencyRepository.getByCode(fromAccount.getCurrencyType()));
                bankTransaction.setAmount(transaction.getAmount());
                bankTransaction.setDescription("OTC transakcija");
                bankTransaction.setTimestamp(Instant.now().toEpochMilli());
                bankTransaction.setFromAccountId(fromAccount);
                bankTransaction.setToAccountId(toAccount);
                bankTransaction.setTransfer(transfer);

                transactionRepository.save(bankTransaction);

                otcTransactionRepository.delete(transaction);
                otcTransactionRepository.flush();
                log.info("Finished transaction " + uid);
            } else if(transaction.getAmountGiven() > 0) {
                log.info("Consistency check for " + uid);

                if(!transaction.getAmountGiven().equals(transaction.getAmountTaken()) || !transaction.getAmountTaken().equals(transaction.getAmount()) || !transaction.getAmountGiven().equals(transaction.getAmount()))
                    throw new Exception("Inconsistency found, rolling back...");

                transaction.setFinished(true);

                otcTransactionRepository.saveAndFlush(transaction);
                nextStage(transaction);
            } else if(transaction.getAmountTaken() > 0) {
                log.info("Transfer funds for " + uid);

                transaction.getSellerAccount().setBalance(transaction.getSellerAccount().getBalance() + transaction.getAmount());
                transaction.setAmountGiven(transaction.getAmount());

                accountRepository.save(transaction.getSellerAccount());
                otcTransactionRepository.saveAndFlush(transaction);
                nextStage(transaction);
            } else {
                log.info("Reserve funds for " + uid);

                if(transaction.getBuyerAccount().getBalance() < transaction.getAmount())
                    throw new Exception("Insufficient funds");

                transaction.getBuyerAccount().setBalance(transaction.getBuyerAccount().getBalance() - transaction.getAmount());
                transaction.setAmountTaken(transaction.getAmount());

                accountRepository.save(transaction.getBuyerAccount());
                otcTransactionRepository.saveAndFlush(transaction);
                nextStage(transaction);
            }
        } catch(Exception e) {
            if(transaction != null) {
                transaction.setFailed(true);
                otcTransactionRepository.saveAndFlush(transaction);
            }
            retryableFailureMessage(uid, e.getMessage(), true);
        }
    }

    public synchronized void initiate(String uid, Long sellerAccountId, Long buyerAccountId, Double amount) {
        try {
            OTCTransaction transaction = new OTCTransaction();

            transaction.setAmount(amount);
            transaction.setUid(uid);
            transaction.setBuyerAccount(accountRepository.findById(buyerAccountId).orElseThrow());
            transaction.setSellerAccount(accountRepository.findById(sellerAccountId).orElseThrow());

            otcTransactionRepository.saveAndFlush(transaction);

            jmsTemplate.convertAndSend(destinationOtcAck, messageHelper.createTextMessage(new OTCTransactionACKDTO(
                    uid, false, ""
            )));
        } catch (Exception e) {
            retryableFailureMessage(uid, e.getMessage(), false);
        }
    }

    public void payPremium(Long fromAccountId, Long toAccountId, Double amount) {
        try {
            Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
            Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

            if(fromAccount.getCurrencyType() != toAccount.getCurrencyType())
                throw new Exception("Currency type mismatch");

            if(fromAccount.getBalance() < amount)
                throw new Exception("Insufficient funds");

            fromAccount.setBalance(fromAccount.getBalance() - amount);
            toAccount.setBalance(toAccount.getBalance() + amount);

            MoneyTransferDTO dto = new MoneyTransferDTO();
            CustomerDTO customer = userServiceCustomer.getCustomerById(toAccount.getOwnerID());

            dto.setAdress(customer.getAddress());
            dto.setAmount(amount);
            dto.setReceiver(customer.getFirstName() + " " + customer.getLastName());
            dto.setRecipientAccount(toAccount.getAccountNumber());
            dto.setFromAccountNumber(fromAccount.getAccountNumber());
            dto.setPayementDescription("Premija za OTC transakciju");
            dto.setPayementCode("403");
            dto.setPayementReference(null);

            Transfer transfer = transferService.createMoneyTransferEntity(
                    fromAccount,
                    toAccount,
                    dto
            );

            transfer.setStatus(TransferStatus.COMPLETED);

            Transaction bankTransaction = new Transaction();

            bankTransaction.setBankOnly(false);
            bankTransaction.setFinalAmount(amount);
            bankTransaction.setFee(0.0);
            bankTransaction.setCurrency(currencyRepository.getByCode(fromAccount.getCurrencyType()));
            bankTransaction.setAmount(amount);
            bankTransaction.setDescription("Premija za OTC transakciju");
            bankTransaction.setTimestamp(Instant.now().toEpochMilli());
            bankTransaction.setFromAccountId(fromAccount);
            bankTransaction.setToAccountId(toAccount);
            bankTransaction.setTransfer(transfer);

            transactionRepository.save(bankTransaction);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            log.info("Premium paid");
        } catch(Exception e) {
            log.error("Unable to pay premium: " + e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rollbackUnfinishedTransactions() {
        List<OTCTransaction> transactions = otcTransactionRepository.findAll();
        for(OTCTransaction transaction : transactions) {
            log.info("Rolling back " + transaction.getUid());
            transaction.setFailed(true);
            otcTransactionRepository.saveAndFlush(transaction);

            retryableFailureMessage(transaction.getUid(), "Crash recovery", true);
        }
    }
}
