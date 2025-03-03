package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Currency;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.times;

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
        fromAccount.setCurrency(CurrencyType.EUR);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setOwnerID(10L);
        toAccount.setCurrency(CurrencyType.EUR);

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

        customerDTO = new CustomerDTO();
        customerDTO.setId(10L);
        customerDTO.setEmail("test@example.com");
        customerDTO.setFirstName("Test");
        customerDTO.setLastName("User");
    }

    @Test
    void validateInternalTransfer_ValidCase_ReturnsTrue() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateInternalTransfer(internalTransferDTO);

        assertTrue(result);
    }
    @Test
    void validateInternalTransfer_InvalidCurrency_ReturnsFalse() {
        toAccount.setCurrency(CurrencyType.USD);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateInternalTransfer(internalTransferDTO);

        assertFalse(result);
    }
    @Test
    void validateMoneyTransfer_ValidCase_ReturnsTrue() {
        toAccount.setOwnerID(20L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(moneyTransferDTO);

        assertTrue(result);
    }

    @Test
    void validateMoneyTransfer_SameOwner_ReturnsFalse() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(toAccount));

        boolean result = transferService.validateMoneyTransfer(moneyTransferDTO);

        assertFalse(result);
    }


}
