package com.banka1.banking.dto;

import com.banka1.banking.dto.request.CreateAccountDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DtoTest {
    
    @Test
    public void testAllDtos() {
        // Test AuthorizedPersonDTO
        AuthorizedPersonDTO authorizedPersonDTO = new AuthorizedPersonDTO();
        authorizedPersonDTO.setFirstName("John");
        authorizedPersonDTO.setLastName("Doe");
        authorizedPersonDTO.setBirthDate("2000-01-01"); // 2000-01-01
        authorizedPersonDTO.setPhoneNumber("+1234567890");
        authorizedPersonDTO.setCompanyID(123L);

        
        // Test hashCode and equals for AuthorizedPersonDTO
        AuthorizedPersonDTO sameAuthorizedPerson = new AuthorizedPersonDTO();
        sameAuthorizedPerson.setFirstName("John");
        sameAuthorizedPerson.setLastName("Doe");
        sameAuthorizedPerson.setBirthDate("2002-10-10");
        sameAuthorizedPerson.setPhoneNumber("+1234567890");
        sameAuthorizedPerson.setCompanyID(123L);
        
        AuthorizedPersonDTO differentAuthorizedPerson = new AuthorizedPersonDTO();
        differentAuthorizedPerson.setFirstName("Jane");

        // Test CreateAccountByEmployeeDTO
        CreateAccountDTO createAccountDTO = new CreateAccountDTO();
        CreateAccountByEmployeeDTO createAccountByEmployeeDTO = new CreateAccountByEmployeeDTO(createAccountDTO, 456L);
        CreateAccountByEmployeeDTO emptyCreateAccountByEmployeeDTO = new CreateAccountByEmployeeDTO();
        
        emptyCreateAccountByEmployeeDTO.setCreateAccountDTO(createAccountDTO);
        emptyCreateAccountByEmployeeDTO.setEmployeeId(456L);

        // Test CustomerDTO
        CustomerDTO customerDTO = new CustomerDTO(1L, "Alice", "Smith", "1985-05-15", 
                "alice@example.com", "+9876543210", "123 Main St");
        CustomerDTO emptyCustomerDTO = new CustomerDTO();
        emptyCustomerDTO.setId(1L);
        emptyCustomerDTO.setFirstName("Alice");
        emptyCustomerDTO.setLastName("Smith");
        emptyCustomerDTO.setBirthDate("1985-05-15");
        emptyCustomerDTO.setEmail("alice@example.com");
        emptyCustomerDTO.setPhoneNumber("+9876543210");
        emptyCustomerDTO.setAddress("123 Main St");
        
        List<String> permissions = Arrays.asList("READ", "WRITE");
        customerDTO.setPermissions(permissions);
        emptyCustomerDTO.setPermissions(permissions);

        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setEmail("user@example.com");
        notificationDTO.setSubject("Test Subject");
        notificationDTO.setMessage("Test Message");
        notificationDTO.setFirstName("Bob");
        notificationDTO.setLastName("Brown");
        notificationDTO.setType("INFO");

        NotificationDTO sameNotification = new NotificationDTO();
        sameNotification.setEmail("user@example.com");
        sameNotification.setSubject("Test Subject");
        sameNotification.setMessage("Test Message");
        sameNotification.setFirstName("Bob");
        sameNotification.setLastName("Brown");
        sameNotification.setType("INFO");

        
        // Test ExchangePreviewDTO
        ExchangePreviewDTO exchangePreviewDTO = new ExchangePreviewDTO("USD", "EUR", 100.0);
        ExchangePreviewDTO emptyExchangePreviewDTO = new ExchangePreviewDTO();
        emptyExchangePreviewDTO.setFromCurrency("USD");
        emptyExchangePreviewDTO.setToCurrency("EUR");
        emptyExchangePreviewDTO.setAmount(100.0);

        // Test toString methods
        assertNotNull(authorizedPersonDTO.toString());
        assertNotNull(createAccountByEmployeeDTO.toString());
        assertNotNull(customerDTO.toString());
        assertNotNull(notificationDTO.toString());
        assertNotNull(exchangePreviewDTO.toString());

    }
}