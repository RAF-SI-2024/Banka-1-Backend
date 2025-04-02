package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.*;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.jms.TextMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private UserServiceCustomer userServiceCustomer;

    @Mock
    private OtpTokenService otpTokenService;

    @Mock
    private BankAccountUtils bankAccountUtils;

    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private Transfer internalTransfer;
    private Transfer externalTransfer;
    private Transfer exchangeTransfer;
    private Transfer foreignTransfer;
    private Account fromAccountForeign;
    private Account toAccountForeign;
    private Account bankAccountUSD;
    private Account bankAccountEUR;
    private Currency usdCurrency;
    private Currency eurCurrency;
    private CustomerDTO customerDTO;
    private CustomerDTO customerDTO2;
    private Transfer pendingTransfer;
    private Company bankCompany;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transferService, "destinationEmail", "email.queue");

        bankCompany = new Company();
        bankCompany.setName("Banka");

        // Setup test accounts
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setOwnerID(100L);
        fromAccount.setAccountNumber("123456789");
        fromAccount.setBalance(1000.0);
        fromAccount.setCurrencyType(CurrencyType.USD);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setOwnerID(200L); // Different owner
        toAccount.setAccountNumber("987654321");
        toAccount.setBalance(500.0);
        toAccount.setCurrencyType(CurrencyType.USD);

        fromAccountForeign = new Account();
        fromAccountForeign.setId(2L);
        fromAccountForeign.setOwnerID(100L);
        fromAccountForeign.setAccountNumber("223456789");
        fromAccountForeign.setBalance(1000.0);
        fromAccountForeign.setCurrencyType(CurrencyType.EUR);

        toAccountForeign = new Account();
        toAccountForeign.setId(3L);
        toAccountForeign.setOwnerID(200L);
        toAccountForeign.setAccountNumber("987654322");
        toAccountForeign.setBalance(500.0);
        toAccountForeign.setCurrencyType(CurrencyType.EUR);

        bankAccountEUR = new Account();
        bankAccountEUR.setId(100L);
        bankAccountEUR.setOwnerID(1L);
        bankAccountEUR.setAccountNumber("111111111");
        bankAccountEUR.setBalance(1000000.0);
        bankAccountEUR.setCurrencyType(CurrencyType.EUR);
        bankAccountEUR.setCompany(bankCompany);

        bankAccountUSD = new Account();
        bankAccountUSD.setId(100L);
        bankAccountUSD.setOwnerID(1L);
        bankAccountUSD.setAccountNumber("111111112");
        bankAccountUSD.setBalance(1000000.0);
        bankAccountUSD.setCurrencyType(CurrencyType.USD);
        bankAccountUSD.setCompany(bankCompany);

        // Setup currencies
        usdCurrency = new Currency();
        usdCurrency.setCode(CurrencyType.USD);

        eurCurrency = new Currency();
        eurCurrency.setCode(CurrencyType.EUR);

        // Setup customer data
        customerDTO = new CustomerDTO();
        customerDTO.setId(100L);
        customerDTO.setFirstName("John");
        customerDTO.setLastName("Doe");
        customerDTO.setEmail("john.doe@example.com");

        customerDTO2 = new CustomerDTO();
        customerDTO2.setId(200L);
        customerDTO2.setFirstName("Jane");
        customerDTO2.setLastName("Doe");
        customerDTO2.setEmail("jane.doe@example.com");

        // Setup pending transfer
        pendingTransfer = new Transfer();
        pendingTransfer.setId(1L);
        pendingTransfer.setFromAccountId(fromAccount);
        pendingTransfer.setToAccountId(toAccount);
        pendingTransfer.setAmount(100.0);
        pendingTransfer.setStatus(TransferStatus.PENDING);
        pendingTransfer.setCreatedAt(System.currentTimeMillis() - 1000); // Created 1 second ago

        // Setup internal transfer
        internalTransfer = new Transfer();
        internalTransfer.setId(1L);
        internalTransfer.setFromAccountId(fromAccount);
        internalTransfer.setToAccountId(toAccount);
        internalTransfer.setAmount(100.0);
        internalTransfer.setStatus(TransferStatus.PENDING);
        internalTransfer.setType(TransferType.INTERNAL);
        internalTransfer.setFromCurrency(usdCurrency);
        internalTransfer.setToCurrency(usdCurrency);

        // Setup external transfer
        externalTransfer = new Transfer();
        externalTransfer.setId(2L);
        externalTransfer.setFromAccountId(fromAccount);
        externalTransfer.setToAccountId(toAccount);
        externalTransfer.setAmount(100.0);
        externalTransfer.setStatus(TransferStatus.PENDING);
        externalTransfer.setType(TransferType.EXTERNAL);
        externalTransfer.setFromCurrency(usdCurrency);
        externalTransfer.setToCurrency(usdCurrency);

        foreignTransfer = new Transfer();
        foreignTransfer.setId(3L);
        foreignTransfer.setFromAccountId(fromAccount);
        foreignTransfer.setToAccountId(toAccountForeign);
        foreignTransfer.setAmount(100.0);
        foreignTransfer.setStatus(TransferStatus.PENDING);
        foreignTransfer.setType(TransferType.FOREIGN);
        foreignTransfer.setFromCurrency(usdCurrency);
        foreignTransfer.setToCurrency(eurCurrency);

        exchangeTransfer = new Transfer();
        exchangeTransfer.setId(4L);
        exchangeTransfer.setFromAccountId(fromAccountForeign);
        exchangeTransfer.setToAccountId(fromAccount);
        exchangeTransfer.setAmount(100.0);
        exchangeTransfer.setStatus(TransferStatus.PENDING);
        exchangeTransfer.setType(TransferType.EXCHANGE);
        exchangeTransfer.setFromCurrency(eurCurrency);
        exchangeTransfer.setToCurrency(usdCurrency);
    }

    @Test
    void testCreateInternalTransfer_Success() {
        toAccount.setOwnerID(100L);

        InternalTransferDTO dto = new InternalTransferDTO();
        dto.setFromAccountId(fromAccount.getId());
        dto.setToAccountId(toAccount.getId());
        dto.setAmount(100.0);

        // Setup mocks
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(usdCurrency));
        when(userServiceCustomer.getCustomerById(100L)).thenReturn(customerDTO);
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(otpTokenService.generateOtp(1L)).thenReturn("123456");

        // Execute
        Long result = transferService.createInternalTransfer(dto);

        // Verify
        assertEquals(1L, result);

        // Verify transfer was created and saved
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).saveAndFlush(transferCaptor.capture());

        Transfer savedTransfer = transferCaptor.getValue();
        assertEquals(fromAccount, savedTransfer.getFromAccountId());
        assertEquals(toAccount, savedTransfer.getToAccountId());
        assertEquals(100.0, savedTransfer.getAmount());
        assertEquals(TransferStatus.PENDING, savedTransfer.getStatus());
        assertEquals(usdCurrency, savedTransfer.getFromCurrency());
        assertEquals(usdCurrency, savedTransfer.getToCurrency());

        // Verify OTP was generated and set
        verify(otpTokenService).generateOtp(1L);
        verify(transferRepository).save(transferCaptor.capture());
        assertEquals("123456", transferCaptor.getValue().getOtp());

        // Verify notification was sent
        ArgumentCaptor<NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messageHelper, times(2)).createTextMessage(notificationCaptor.capture());

        NotificationDTO sentNotification = notificationCaptor.getValue();
        assertEquals("Verifikacija", sentNotification.getSubject());
        assertEquals("john.doe@example.com", sentNotification.getEmail());
        assertTrue(sentNotification.getMessage().contains("Kliknite"));
        assertEquals("John", sentNotification.getFirstName());
        assertEquals("Doe", sentNotification.getLastName());
    }

    @Test
    void testCreateMoneyTransfer_ExternalTransfer_Success() {
        // Setup test data
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setFromAccountNumber("123456789");
        dto.setRecipientAccount("987654321");
        dto.setAmount(100.0);
        dto.setReceiver("Jane Smith");
        dto.setPayementCode("123");
        dto.setPayementDescription("Payment for services");

        // Setup mocks
        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(usdCurrency));
        when(userServiceCustomer.getCustomerById(100L)).thenReturn(customerDTO);
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(otpTokenService.generateOtp(1L)).thenReturn("123456");

        // Execute
        Long result = transferService.createMoneyTransfer(dto);

        // Verify
        assertEquals(1L, result);

        // Verify transfer was created and saved
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).saveAndFlush(transferCaptor.capture());

        Transfer savedTransfer = transferCaptor.getValue();
        assertEquals(fromAccount, savedTransfer.getFromAccountId());
        assertEquals(toAccount, savedTransfer.getToAccountId());
        assertEquals(100.0, savedTransfer.getAmount());
        assertEquals("Jane Smith", savedTransfer.getReceiver());
        assertEquals(TransferStatus.PENDING, savedTransfer.getStatus());
        assertEquals(usdCurrency, savedTransfer.getFromCurrency());
        assertEquals(usdCurrency, savedTransfer.getToCurrency());
        assertEquals("123", savedTransfer.getPaymentCode());
        assertEquals("Payment for services", savedTransfer.getPaymentDescription());

        // Verify OTP was generated and set
        verify(otpTokenService).generateOtp(1L);
        verify(transferRepository).save(transferCaptor.capture());
        assertEquals("123456", transferCaptor.getValue().getOtp());

        // Verify notification was sent
        ArgumentCaptor<NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messageHelper, times(2)).createTextMessage(notificationCaptor.capture());

        NotificationDTO sentNotification = notificationCaptor.getValue();
        assertEquals("Verifikacija", sentNotification.getSubject());
        assertEquals("john.doe@example.com", sentNotification.getEmail());
        assertTrue(sentNotification.getMessage().contains("Kliknite"));
        assertEquals("John", sentNotification.getFirstName());
        assertEquals("Doe", sentNotification.getLastName());
    }

    @Test
    void testCreateMoneyTransfer_ForeignTransfer_Success() {
        // Change to account for foreign transfer
        toAccount.setCurrencyType(CurrencyType.EUR);

        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setFromAccountNumber("123456789");
        dto.setRecipientAccount("987654321");
        dto.setAmount(100.0);
        dto.setReceiver("Jane Smith");
        dto.setPayementCode("123");
        dto.setPayementDescription("International payment");

        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(usdCurrency));
        when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(eurCurrency));
        when(userServiceCustomer.getCustomerById(100L)).thenReturn(customerDTO);
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(otpTokenService.generateOtp(1L)).thenReturn("123456");

        Long result = transferService.createMoneyTransfer(dto);

        assertEquals(1L, result);

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).saveAndFlush(transferCaptor.capture());

        assertEquals(usdCurrency, transferCaptor.getValue().getFromCurrency());
        assertEquals(eurCurrency, transferCaptor.getValue().getToCurrency());
    }

    @Test
    void testCreateMoneyTransfer_AccountNotFound() {
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setFromAccountNumber("111111111"); // Non-existent account
        dto.setRecipientAccount("987654321");

        when(accountRepository.findByAccountNumber("111111111")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(toAccount));

        Long result = transferService.createMoneyTransfer(dto);

        assertNull(result);
        verify(transferRepository, never()).saveAndFlush(any(Transfer.class));
    }

    @Test
    void testValidateMoneyTransfer_Success() {
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setFromAccountNumber("123456789");
        dto.setRecipientAccount("987654321");

        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(dto);

        assertTrue(result);
    }

    @Test
    void testValidateMoneyTransfer_SameOwner() {
        // Set same owner for both accounts
        toAccount.setOwnerID(100L);

        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setFromAccountNumber("123456789");
        dto.setRecipientAccount("987654321");

        when(accountRepository.findByAccountNumber("123456789")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumber("987654321")).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(dto);

        // Should fail because accounts belong to same owner
        assertFalse(result);
    }

    @Test
    void testValidateInternalTransfer_Success() {
        // Set same owner for both accounts for internal transfer
        toAccount.setOwnerID(100L);

        InternalTransferDTO dto = new InternalTransferDTO();
        dto.setFromAccountId(1L);
        dto.setToAccountId(2L);
        dto.setAmount(100.0);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateInternalTransfer(dto);

        assertTrue(result);
    }

    @Test
    void testCancelExpiredTransfers() {
        // Create a list of expired transfers
        List<Transfer> expiredTransfers = Arrays.asList(pendingTransfer);

        // Setup mock to return expired transfers
        when(transferRepository.findAllByStatusAndCreatedAtBefore(
                eq(TransferStatus.PENDING), anyLong())).thenReturn(expiredTransfers);

        // Execute
        transferService.cancelExpiredTransfers();

        // Verify
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());

        assertEquals(TransferStatus.CANCELLED, transferCaptor.getValue().getStatus());
    }

    @Test
    void testFindById_Success() {
        when(transferRepository.findById(1L)).thenReturn(Optional.of(pendingTransfer));

        Transfer result = transferService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testFindById_NotFound() {
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> transferService.findById(999L));
    }

    @Test
    void testProcessTransfer_Internal() {
        when(transferRepository.findById(1L)).thenReturn(Optional.of(internalTransfer));
        when(accountRepository.save(any(Account.class))).thenReturn(null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        String result = transferService.processTransfer(1L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, internalTransfer.getStatus());
        assertEquals(900.0, fromAccount.getBalance());
        assertEquals(600.0, toAccount.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testProcessTransfer_Exchange() {
        when(transferRepository.findById(4L)).thenReturn(Optional.of(exchangeTransfer));
        when(accountRepository.save(any(Account.class))).thenReturn(null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);
        when(userServiceCustomer.getCustomerById(100L)).thenReturn(customerDTO);

        when(exchangeService.calculatePreviewExchangeAutomatic(anyString(), anyString(), any())).thenReturn(
                Map.of(
                        "finalAmount", 90.0
                )
        );
        when(bankAccountUtils.getBankAccountForCurrency(eurCurrency.getCode())).thenReturn(bankAccountEUR);
        when(bankAccountUtils.getBankAccountForCurrency(usdCurrency.getCode())).thenReturn(bankAccountUSD);

        String result = transferService.processTransfer(4L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, exchangeTransfer.getStatus());
        assertEquals(900.0, fromAccountForeign.getBalance());
        assertEquals(1090.0, fromAccount.getBalance());
        assertEquals(1000100.0, bankAccountEUR.getBalance());
        assertEquals(999910.0, bankAccountUSD.getBalance());
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    void testProcessTransfer_External() {
        when(transferRepository.findById(2L)).thenReturn(Optional.of(externalTransfer));
        when(accountRepository.save(any(Account.class))).thenReturn(null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);

        String result = transferService.processTransfer(2L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, externalTransfer.getStatus());
        assertEquals(900.0, fromAccount.getBalance());
        assertEquals(600.0, toAccount.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testProcessTransfer_Foreign() {
        when(transferRepository.findById(3L)).thenReturn(Optional.of(foreignTransfer));
        when(accountRepository.save(any(Account.class))).thenReturn(null);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(null);
        when(userServiceCustomer.getCustomerById(200L)).thenReturn(customerDTO2);

        when(exchangeService.calculatePreviewExchangeAutomatic(anyString(), anyString(), any())).thenReturn(
                Map.of(
                        "finalAmount", 90.0
                )
        );
        when(bankAccountUtils.getBankAccountForCurrency(eurCurrency.getCode())).thenReturn(bankAccountEUR);
        when(bankAccountUtils.getBankAccountForCurrency(usdCurrency.getCode())).thenReturn(bankAccountUSD);

        String result = transferService.processTransfer(3L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, foreignTransfer.getStatus());
        assertEquals(900.0, fromAccount.getBalance());
        assertEquals(590.0, toAccountForeign.getBalance());
        assertEquals(1000100.0, bankAccountUSD.getBalance());
        assertEquals(999910.0, bankAccountEUR.getBalance());
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    void testProcessInternalTransfer_InsufficientFunds() {
        fromAccount.setBalance(50.0);
        internalTransfer.setAmount(100.0);

        when(transferRepository.findById(1L)).thenReturn(Optional.of(internalTransfer));

        assertThrows(RuntimeException.class, () -> transferService.processInternalTransfer(1L));
        assertEquals(TransferStatus.FAILED, internalTransfer.getStatus());
    }

    @Test
    void testProcessExternalTransfer_InsufficientFunds() {
        fromAccount.setBalance(50.0);
        externalTransfer.setAmount(100.0);

        when(transferRepository.findById(2L)).thenReturn(Optional.of(externalTransfer));

        assertThrows(RuntimeException.class, () -> transferService.processExternalTransfer(2L));
        assertEquals(TransferStatus.FAILED, externalTransfer.getStatus());
    }
}