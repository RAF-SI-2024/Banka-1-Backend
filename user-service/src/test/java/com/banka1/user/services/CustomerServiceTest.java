package com.banka1.user.services;

import com.banka1.user.DTO.CustomerDTO.CustomerDTO;
import com.banka1.user.mapper.CustomerMapper;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void testCreateCustomer() {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setPassword("1234");
        Customer customer = CustomerMapper.dtoToCustomer(customerDTO);

        when(passwordEncoder.encode(anyString())).thenReturn("####");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer createdCustomer = customerService.createCustomer(customerDTO);

        assertNotNull(createdCustomer);
        assertEquals("####", createdCustomer.getPassword());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testUpdateCustomer() {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setIme("Petar");

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
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setIme("Petar");

        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

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

        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            customerService.deleteCustomer(1L);
        });

 //       assertEquals("Korisnik nije pronađen", exception.getMessage());
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
        when(customerRepository.findById(1L)).thenReturn(Optional.of(new Customer()));

        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            customerService.updateCustomerPermissions(1L, null);
        });

        assertEquals("Lista permisija ne može biti prazna ili null", exception.getMessage());

        exception = assertThrows(ResponseStatusException.class, () -> {
            customerService.updateCustomerPermissions(1L, List.of());
        });

        assertEquals("Lista permisija ne može biti prazna ili null", exception.getMessage());
    }
}
