package com.banka1.banking.service.accountService;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.helper.AccountStatus;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.services.AccountService;
import com.banka1.banking.services.CardService;
import com.banka1.banking.services.UserServiceCustomer;
import jakarta.jms.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateAccountTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ModelMapper modelMapper;
    @InjectMocks
    private AccountService accountService;
    @Mock
    private UserServiceCustomer userServiceCustomer;
    @Mock
    private CardService cardService;
    @Mock
    JmsTemplate jmsTemplate;
    @Mock
    MessageHelper messageHelper;

    /*
    {
      "ownerID": 0,
      "currency": "RSD",
      "type": "CURRENT",
      "subtype": "PERSONAL",
      "dailyLimit": 0,
      "monthlyLimit": 0,
      "status": "ACTIVE"
    }
*/

    private CreateAccountDTO createAccountDTO;
    private Account acc;
    private CustomerDTO customerDTO;

    @BeforeEach
    void setup() {
        createAccountDTO = new CreateAccountDTO();
        createAccountDTO.setOwnerID(1L);
        createAccountDTO.setCurrency(CurrencyType.RSD);
        createAccountDTO.setType(AccountType.CURRENT);
        createAccountDTO.setSubtype(AccountSubtype.PERSONAL);
        createAccountDTO.setDailyLimit(0.0);
        createAccountDTO.setMonthlyLimit(0.0);
        createAccountDTO.setStatus(AccountStatus.ACTIVE);
        createAccountDTO.setCreateCard(true);

        acc = new Account();
        acc.setType(AccountType.CURRENT);
        acc.setOwnerID(1L);
        acc.setCurrencyType(CurrencyType.RSD);
        acc.setType(AccountType.CURRENT);
        acc.setSubtype(AccountSubtype.PERSONAL);
        acc.setDailyLimit(0.0);
        acc.setMonthlyLimit(0.0);
        acc.setStatus(AccountStatus.ACTIVE);
        acc.setAccountNumber("111000112345678911");

        customerDTO = new CustomerDTO();
        customerDTO.setId(1L);

    }
    @Test
    public void createAccountSuccesfullyTest() {

        when(modelMapper.map(createAccountDTO, Account.class)).thenReturn(acc);

        when(accountRepository.save(acc)).thenReturn(acc);

        when(userServiceCustomer.getCustomerById(anyLong())).thenReturn(customerDTO);

        when(cardService.createCard(any())).thenReturn(new Card());

        when(messageHelper.createTextMessage(any())).thenReturn("a");

        doNothing().when(jmsTemplate).convertAndSend((String) any(), anyString());

        Account result = accountService.createAccount(createAccountDTO, 1L);

        assertNotNull(result);
        assertEquals(acc.getAccountNumber().substring(0,7), result.getAccountNumber().substring(0,7));
        assertEquals(acc.getAccountNumber().substring(16,18), result.getAccountNumber().substring(16,18));
        verify(accountRepository, times(1)).save(acc);
    }

    @Test
    public void createAccountBadCombinationTest() {
        createAccountDTO.setCurrency(CurrencyType.EUR);

        when(userServiceCustomer.getCustomerById(anyLong())).thenReturn(customerDTO);
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            accountService.createAccount(createAccountDTO, 1L);
        });
        assertEquals("400 BAD_REQUEST \"Nevalidna kombinacija vrste racuna i valute\"", exception.getMessage());

        verify(accountRepository, never()).save(any());
    }

}
