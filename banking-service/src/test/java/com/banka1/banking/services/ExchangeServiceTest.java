package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
import com.banka1.banking.models.ExchangePair;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.CurrencyRepository;
import com.banka1.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.banka1.banking.repository.ExchangePairRepository;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeServiceTest {

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

    @Mock
    private ExchangePairRepository exchangePairRepository;

    @InjectMocks
    private ExchangeService exchangeService;

    private Account fromAccount;
    private Account toAccount;
    private Currency currencyEUR;
    private Currency currencyUSD;
    private ExchangeMoneyTransferDTO exchangeMoneyTransferDTO;
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
        toAccount.setCurrencyType(CurrencyType.USD);

        currencyEUR = new Currency();
        currencyEUR.setCode(CurrencyType.EUR);

        currencyUSD = new Currency();
        currencyUSD.setCode(CurrencyType.USD);

        exchangeMoneyTransferDTO = new ExchangeMoneyTransferDTO();
        exchangeMoneyTransferDTO.setAccountFrom(1L);
        exchangeMoneyTransferDTO.setAccountTo(2L);
        exchangeMoneyTransferDTO.setAmount(500.0);
         customerDTO = new CustomerDTO(10L,"Marko","Markovic","2025-01-01","test@test.com","0101010101","MARSALA TULBUHINA");


        ReflectionTestUtils.setField(exchangeService, "destinationEmail", "test-destination");
    }

    @Test
    void validateExchangeTransferValidCaseReturnsTrue() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = exchangeService.validateExchangeTransfer(exchangeMoneyTransferDTO);
        assertTrue(result);
    }

    @Test
    void validateExchangeTransferSameCurrencyReturnsFalse() {
        toAccount.setCurrencyType(CurrencyType.EUR);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = exchangeService.validateExchangeTransfer(exchangeMoneyTransferDTO);
        assertFalse(result);
    }

    @Test
    void createExchangeTransferSuccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(currencyRepository.findByCode(CurrencyType.EUR)).thenReturn(Optional.of(currencyEUR));
        when(currencyRepository.findByCode(CurrencyType.USD)).thenReturn(Optional.of(currencyUSD));
        when(userServiceCustomer.getCustomerById(10L)).thenReturn(customerDTO);
        doReturn("123456").when(otpTokenService).generateOtp(anyLong());

        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer savedTransfer = invocation.getArgument(0);
            savedTransfer.setId(102L);
            return savedTransfer;
        });

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doReturn("Simulirana poruka").when(messageHelper).createTextMessage(any(NotificationDTO.class));

        exchangeService.createExchangeTransfer(exchangeMoneyTransferDTO);

        verify(transferRepository, times(1)).saveAndFlush(any(Transfer.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(jmsTemplate, times(2)).convertAndSend(eq("test-destination"), eq("Simulirana poruka"));
    }

    @Test
    void calculatePreviewExchange_withDirectPair() {
        ExchangePair pair = new ExchangePair();
        pair.setExchangeRate(117.2332942555686);
        when(exchangePairRepository.findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType.RSD, CurrencyType.EUR))
                .thenReturn(Optional.of(pair));

        Map<String, Object> result = exchangeService.calculatePreviewExchange("RSD", "EUR", 1000.0);

        assertEquals(1 / 117.2332942555686, (Double) result.get("exchangeRate"), 0.001);
        assertNotNull(result.get("convertedAmount"));
        assertNotNull(result.get("fee"));
        assertNotNull(result.get("provision"));
        assertNotNull(result.get("finalAmount"));
    }

    @Test
    void calculatePreviewExchangeForeign_withBothDirectPairs() {
        // FROM -> RSD
        ExchangePair usdToRsd = new ExchangePair();
        usdToRsd.setExchangeRate(108.0);
        when(exchangePairRepository.findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType.USD, CurrencyType.RSD))
                .thenReturn(Optional.of(usdToRsd));

        // RSD -> EUR
        ExchangePair rsdToEur = new ExchangePair();
        rsdToEur.setExchangeRate(117.0);
        when(exchangePairRepository.findByBaseCurrencyCodeAndTargetCurrencyCode(CurrencyType.RSD, CurrencyType.EUR))
                .thenReturn(Optional.of(rsdToEur));

        // Act
        Map<String, Object> result = exchangeService.calculatePreviewExchangeForeign("USD", "EUR", 100.0);

        // Assert
        assertEquals(108.0, result.get("firstExchangeRate"));
        assertEquals(1 / 117.0, (Double) result.get("secondExchangeRate"), 0.001);
        assertNotNull(result.get("totalFee"));
        assertNotNull(result.get("finalAmount"));
    }
}

