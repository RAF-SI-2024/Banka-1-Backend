package com.banka1.user.services;

import com.banka1.user.DTO.banking.CreateAccountWithoutOwnerIdDTO;
import com.banka1.user.DTO.banking.helper.AccountStatus;
import com.banka1.user.DTO.banking.helper.AccountSubtype;
import com.banka1.user.DTO.banking.helper.AccountType;
import com.banka1.user.DTO.banking.helper.CurrencyType;
import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.request.UpdateCustomerRequest;
import com.banka1.user.DTO.response.CustomerPageResponse;
import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Permission;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.service.CustomerService;
import com.banka1.user.service.SetPasswordService;
import org.hibernate.annotations.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.AssertionErrors;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SetPasswordService setPasswordService;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void testCreateCustomer() {
        var createAccountDTO = new CreateAccountWithoutOwnerIdDTO();
        createAccountDTO.setCurrency(CurrencyType.RSD);
        createAccountDTO.setType(AccountType.CURRENT);
        createAccountDTO.setSubtype(AccountSubtype.PERSONAL);
        createAccountDTO.setDailyLimit(0.0);
        createAccountDTO.setMonthlyLimit(0.0);
        createAccountDTO.setStatus(AccountStatus.ACTIVE);

        var customerDTO = new CreateCustomerRequest();
        customerDTO.setFirstName("Petar");
        customerDTO.setLastName("Petrovic");
        customerDTO.setUsername("ppetrovic");
        customerDTO.setAddress("Ulica");
        customerDTO.setEmail("ppetrovic@example.com");
        customerDTO.setGender(Gender.MALE);
        customerDTO.setBirthDate("2000-03-03");
        customerDTO.setPhoneNumber("555333");
        customerDTO.setAccountInfo(createAccountDTO);


        when(customerRepository.save(any(Customer.class)))
                .then(invocation -> {
                    var savedCustomer = (Customer) invocation.getArguments()[0];
                    savedCustomer.setId(1L);
                    return savedCustomer;
                });

        Customer createdCustomer = customerService.createCustomer(customerDTO, 1L);

        assertNotNull(createdCustomer);
        assertNull(createdCustomer.getPassword());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testUpdateCustomer() {
        var customerDTO = new UpdateCustomerRequest();
        customerDTO.setFirstName("Petar");

        Customer customer = new Customer();
        customer.setId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Optional<Customer> updatedCustomer = customerService.updateCustomer(1L, customerDTO);

        assertNotNull(updatedCustomer);
        assertEquals("Petar", updatedCustomer.get().getFirstName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testUpdateCustomerNotFound() {
        var customerDTO = new UpdateCustomerRequest();
        customerDTO.setFirstName("Petar");

        //when(customerRepository.findById(1L)).thenReturn(Optional.empty());

//      assertThrows(...)
    }

    @Test
    void testDeleteCustomer(){
        Customer customer = new Customer();
        customer.setId(1L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertTrue(customerService.deleteCustomer(1L));
        verify(customerRepository, times(1)).delete(customer);
    }

    @Test
    void testDeleteCustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertFalse(customerService.deleteCustomer(1L));
        verify(customerRepository, times(0)).delete(any(Customer.class));
    }

    @Test
    void testUpdateCustomerPermissions() {
        Customer customer = new Customer();
        customer.setId(1L);

        List<Permission> permissions = List.of(Permission.READ_CUSTOMER);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Optional<Customer> updatedCustomer = customerService.updateCustomerPermissions(1L, permissions);

        assertNotNull(updatedCustomer);
        assertEquals(permissions, updatedCustomer.get().getPermissions());
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void testUpdateCustomerPermissionsInvalid() {
        var exception = assertThrows(ResponseStatusException.class, () -> {
            customerService.updateCustomerPermissions(1L, null);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lista permisija ne može biti prazna ili null", exception.getBody().getDetail());

        exception = assertThrows(ResponseStatusException.class, () -> {
            customerService.updateCustomerPermissions(1L, List.of());
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Lista permisija ne može biti prazna ili null", exception.getBody().getDetail());
    }

    @Test
    void findByIdSuccess() {
        String id = "1";
        var entity = getCustomer();

        Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));

        var response = getCustomerResponse();

        AssertionErrors.assertEquals("Response", response, customerService.findById(id));

        Mockito.verify(customerRepository).findById(1L);
    }

    private static Customer getCustomer() {
        var entity = new Customer();

        entity.setFirstName("Petar");
        entity.setLastName("Petrovic");
        entity.setUsername("ppetrovic");
        entity.setBirthDate("2000-03-03");
        entity.setGender(Gender.MALE);
        entity.setEmail("ppetrovic@banka.rs");
        entity.setPhoneNumber("99999999");
        entity.setAddress("Ulica");
        entity.setPermissions(List.of());
        entity.setId(1L);
        return entity;
    }

    private static CustomerResponse getCustomerResponse() {
        return new CustomerResponse(
                1L,
                "Petar",
                "Petrovic",
                "ppetrovic",
                "2000-03-03",
                Gender.MALE,
                "ppetrovic@banka.rs",
                "99999999",
                "Ulica",
                List.of());
    }

    @Test
    void findByIdNotFound() {
        String id = "1";

        Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        AssertionErrors.assertNull("Response", customerService.findById(id));

        Mockito.verify(customerRepository).findById(1L);
    }

    @Test
    void findByIdInvalidId() {
        String id = "Petar";

        try {
            customerService.findById(id);
            fail("No exception.");
        } catch (Exception e) {
            AssertionErrors.assertNotNull("Error", e);
        }
    }

    @Test
    void search() {
        var response = new CustomerPageResponse(1, List.of(getCustomerResponse()));

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Mockito.when(customerRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(getCustomer()), pageable, 1));

        AssertionErrors.assertEquals("Response", response,
                customerService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        Mockito.verify(customerRepository).findAll(pageable);
    }

    @Test
    void searchNoFilterValue() {
        try {
            customerService.search(0, 10, Optional.empty(), Optional.empty(), Optional.of("firstName"), Optional.empty());
            fail("No exception.");
        } catch (Exception e) {
            AssertionErrors.assertNotNull("Error", e);
        }
    }
}
