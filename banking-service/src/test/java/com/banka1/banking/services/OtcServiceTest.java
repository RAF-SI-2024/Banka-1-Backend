package com.banka1.banking.services;



import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.OTCTransactionACKDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.models.OTCTransaction;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.OTCTransactionRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.common.listener.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class OtcServiceTest {
    @InjectMocks
    private OTCService otcService;
    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private UserServiceCustomer userServiceCustomer;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private TransferService transferService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OTCTransactionRepository otcTransactionRepository;

    @Mock
    private TaskScheduler taskScheduler;

    private final String uid = "uid";

    private OTCTransaction transaction;

    @BeforeEach
    public void setupTransaction() {
        transaction = new OTCTransaction();
        transaction.setUid(uid);
    }


    @Test
    public void testInitiate_Success() {
        Account buyer = new Account();
        buyer.setId(1L);
        buyer.setCurrencyType(CurrencyType.valueOf("USD"));

        Account seller = new Account();
        seller.setId(2L);
        seller.setCurrencyType(CurrencyType.valueOf("USD"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(messageHelper.createTextMessage(any(OTCTransactionACKDTO.class))).thenReturn("ACK");

        otcService.initiate(uid, seller.getId(), buyer.getId(), 100.0);
        verify(otcTransactionRepository).saveAndFlush(any());
    }

    @Test
    public void testProceed_InitializedStage_Success() {

        Account seller = new Account();
        seller.setId(1L);
        seller.setBalance(200.0);
        seller.setCurrencyType(CurrencyType.valueOf("USD"));

        Account buyer = new Account();
        buyer.setId(2L);
        buyer.setBalance(300.0);
        buyer.setCurrencyType(CurrencyType.valueOf("USD"));

        transaction.setAmount(100.0);
        transaction.setBuyerAccount(buyer);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(messageHelper.createTextMessage(any(OTCTransactionACKDTO.class))).thenReturn("ACK");
        when(otcTransactionRepository.saveAndFlush(any())).thenReturn(transaction);
        when(otcTransactionRepository.findByUid(uid)).thenReturn(Optional.ofNullable(transaction));
        otcService.initiate(uid, seller.getId(), buyer.getId(), 100.0);

        otcService.proceed(uid);


        assertEquals(200.0, buyer.getBalance()); // Balance should be reduced by 100
    }

    @Test
    public void testProceed_AssertStage_Success() {

        Account seller = new Account();
        seller.setId(1L);
        seller.setBalance(200.0);
        seller.setCurrencyType(CurrencyType.valueOf("USD"));

        Account buyer = new Account();
        buyer.setId(2L);
        buyer.setBalance(300.0);
        buyer.setCurrencyType(CurrencyType.valueOf("USD"));

        transaction.setSellerAccount(seller);
        transaction.setBuyerAccount(buyer);
        transaction.setAmount(100.0);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(messageHelper.createTextMessage(any(OTCTransactionACKDTO.class))).thenReturn("ACK");
        when(otcTransactionRepository.saveAndFlush(any())).thenReturn(transaction);
        when(otcTransactionRepository.findByUid(uid)).thenReturn(Optional.ofNullable(transaction));
        otcService.initiate(uid, seller.getId(), buyer.getId(), 100.0);

        otcService.proceed(uid);
        otcService.proceed(uid);
        otcService.proceed(uid);

        assertEquals(200.0, buyer.getBalance());
        assertEquals(300.0, seller.getBalance());
        assertEquals(100.0, transaction.getAmountTaken());
        assertEquals(100.0, transaction.getAmountGiven());
    }

    @Test
    public void testRollback_AssetsReserved() {

        Account seller = new Account();
        seller.setId(1L);
        seller.setBalance(200.0);
        seller.setCurrencyType(CurrencyType.valueOf("USD"));

        Account buyer = new Account();
        buyer.setId(2L);
        buyer.setBalance(300.0);
        buyer.setCurrencyType(CurrencyType.valueOf("USD"));

        transaction.setSellerAccount(seller);
        transaction.setBuyerAccount(buyer);
        transaction.setAmount(100.0);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(otcTransactionRepository.saveAndFlush(any())).thenReturn(transaction);
        when(otcTransactionRepository.findByUid(uid)).thenReturn(Optional.ofNullable(transaction));
        otcService.initiate(uid, seller.getId(), buyer.getId(), 50.0);


        otcService.proceed(uid);
        otcService.rollback(uid);

        assertEquals(seller.getBalance(), 200.0);
        assertEquals(buyer.getBalance(), 300.0);
    }

    @Test
    public void testRollback_AssetsTransferred() {
        Account seller = new Account();
        seller.setId(1L);
        seller.setBalance(200.0);
        seller.setCurrencyType(CurrencyType.valueOf("USD"));

        Account buyer = new Account();
        buyer.setId(2L);
        buyer.setBalance(300.0);
        buyer.setCurrencyType(CurrencyType.valueOf("USD"));

        transaction.setSellerAccount(seller);
        transaction.setBuyerAccount(buyer);
        transaction.setAmount(100.0);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(otcTransactionRepository.saveAndFlush(any())).thenReturn(transaction);
        when(otcTransactionRepository.findByUid(uid)).thenReturn(Optional.ofNullable(transaction));
        otcService.initiate(uid, seller.getId(), buyer.getId(), 100.0);

        otcService.proceed(uid);
        otcService.proceed(uid);

        otcService.rollback(uid);

        assertEquals(seller.getBalance(), 200.0);
        assertEquals(buyer.getBalance(), 300.0);
    }

    @Test
    public void testPayPremium_Success() {
        Account from = new Account();
        from.setId(1L);
        from.setBalance(200.0);
        from.setCurrencyType(CurrencyType.valueOf("USD"));

        Account to = new Account();
        to.setId(2L);
        to.setBalance(50.0);
        to.setCurrencyType(CurrencyType.valueOf("USD"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));
        when(userServiceCustomer.getCustomerById(any())).thenReturn(new CustomerDTO());
        when(currencyRepository.getByCode(any())).thenReturn(new Currency());
        when(transferService.createMoneyTransferEntity(eq(from), eq(to), any())).thenReturn(new Transfer());

        otcService.payPremium(1L, 2L, 100.0);

        assertEquals(100.0, from.getBalance());
        assertEquals(150.0, to.getBalance());
        verify(accountRepository).save(from);
        verify(accountRepository).save(to);
    }

    @Test
    public void testPayPremium_InsufficientFunds() {
        Account from = new Account();
        from.setId(1L);
        from.setBalance(20.0);
        from.setCurrencyType(CurrencyType.valueOf("USD"));

        Account to = new Account();
        to.setId(2L);
        to.setCurrencyType(CurrencyType.valueOf("USD"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(to));

        otcService.payPremium(1L, 2L, 100.0);

        verify(accountRepository, never()).save(any());
    }
}
