package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private UserServiceCustomer userServiceCustomer;

    @Mock
    private OtpTokenService otpTokenService;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private Currency currency;
    private InternalTransferDTO internalTransferDTO;
    private MoneyTransferDTO moneyTransferDTO;
    private CustomerDTO customerDTO;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setOwnerID(10L);
        fromAccount.setCurrencyType(CurrencyType.EUR);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setOwnerID(10L);
        toAccount.setCurrencyType(CurrencyType.EUR);

        currency = new Currency();
        currency.setCode(CurrencyType.EUR);

        internalTransferDTO = new InternalTransferDTO();
        internalTransferDTO.setFromAccountId(1L);
        internalTransferDTO.setToAccountId(2L);
        internalTransferDTO.setAmount(500.0);

        moneyTransferDTO = new MoneyTransferDTO();
        moneyTransferDTO.setFromAccountId(1L);
        moneyTransferDTO.setToAccountId(3L);
        moneyTransferDTO.setAmount(300.0);

        customerDTO = new CustomerDTO(10L,"Marko","Markovic",0101L,"test@test.com","0101010101","MARSALA TULBUHINA");

        ReflectionTestUtils.setField(transferService, "destinationEmail", "test-destination");
    }


    @Test
    void validateInternalTransferValidCaseReturnsTrue() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateInternalTransfer(internalTransferDTO);

        assertTrue(result);
    }
    @Test
    void validateInternalTransferInvalidCurrencyReturnsFalse() {
        toAccount.setCurrencyType(CurrencyType.USD);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateInternalTransfer(internalTransferDTO);

        assertFalse(result);
    }
    @Test
    void validateMoneyTransferValidCaseReturnsTrue() {
        toAccount.setOwnerID(20L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(moneyTransferDTO);

        assertTrue(result);
    }

    @Test
    void validateMoneyTransferSameOwnerReturnsFalse() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(moneyTransferDTO);

        assertFalse(result);
    }

    @Test
    void createInternalTransferSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(currency));
        when(userServiceCustomer.getCustomerById(10L)).thenReturn(customerDTO);
        doReturn("123456").when(otpTokenService).generateOtp(anyLong());

        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer savedTransfer = invocation.getArgument(0);
            savedTransfer.setId(100L);
            return savedTransfer;
        });

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doReturn("Simulirana poruka").when(messageHelper).createTextMessage(any(NotificationDTO.class));

        transferService.createInternalTransfer(internalTransferDTO);

        verify(transferRepository, times(1)).saveAndFlush(any(Transfer.class));

        verify(transferRepository, times(1)).save(any(Transfer.class));

        verify(jmsTemplate, times(1)).convertAndSend(eq("test-destination"), eq("Simulirana poruka"));
    }


    @Test
    void createMoneyTransferSuccess() {
        toAccount.setOwnerID(20L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(currency));
        when(userServiceCustomer.getCustomerById(10L)).thenReturn(customerDTO);
        doReturn("123456").when(otpTokenService).generateOtp(anyLong());

        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer savedTransfer = invocation.getArgument(0);
            savedTransfer.setId(101L);
            return savedTransfer;
        });

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doReturn("Simulirana poruka").when(messageHelper).createTextMessage(any(NotificationDTO.class));

        transferService.createMoneyTransfer(moneyTransferDTO);

        verify(transferRepository, times(1)).saveAndFlush(any(Transfer.class));

        verify(transferRepository, times(1)).save(any(Transfer.class));

        verify(jmsTemplate, times(1)).convertAndSend(eq("test-destination"), eq("Simulirana poruka"));
    }


    @Test
    void cancelExpiredTransfersSuccess() {
        Transfer expiredTransfer = new Transfer();
        expiredTransfer.setId(1L);
        expiredTransfer.setStatus(TransferStatus.PENDING);
        expiredTransfer.setCreatedAt(System.currentTimeMillis() - (5 * 6 * 1000));

        when(transferRepository.findAllByStatusAndCreatedAtBefore(eq(TransferStatus.PENDING), anyLong()))
                .thenReturn(List.of(expiredTransfer));

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.cancelExpiredTransfers();

        verify(transferRepository, times(1)).save(any(Transfer.class));
        assertEquals(TransferStatus.CANCELLED, expiredTransfer.getStatus());
    }


}
