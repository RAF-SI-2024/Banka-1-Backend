package com.banka1.user.services;

import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.model.SetPassword;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.repository.SetPasswordRepository;
import com.banka1.user.service.SetPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetPasswordServiceTest {

    @Mock
    private SetPasswordRepository setPasswordRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private SetPasswordService setPasswordService;

    private SetPassword setPassword;
    private final String token = "test-token";
    private final long userId = 1L;

    @BeforeEach
    void setUp() {
        setPassword = SetPassword.builder()
                .token(token)
                .userId(userId)
                .used(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plus(Duration.ofDays(1)))
                .customer(true)
                .build();
    }

    @Test
    void shouldSaveSetPasswordRequest() {
        setPasswordService.saveSetPasswordRequest(token, userId, true);
        verify(setPasswordRepository, times(1)).save(any(SetPassword.class));
    }

    @Test
    void shouldThrowExceptionIfTokenNotFound() {
        when(setPasswordRepository.findByToken(token)).thenReturn(Optional.empty());
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken(token);
        request.setPassword("newPassWord");
        assertThrows(ResponseStatusException.class, () -> setPasswordService.setPassword(request));
    }

    @Test
    void shouldThrowExceptionIfTokenExpired() {
        setPassword.setExpirationDate(Instant.now().minus(Duration.ofDays(1)));
        when(setPasswordRepository.findByToken(token)).thenReturn(Optional.of(setPassword));
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken(token);
        request.setPassword("newPassWord");
        assertThrows(ResponseStatusException.class, () -> setPasswordService.setPassword(request));
    }

    @Test
    void shouldThrowExceptionIfTokenAlreadyUsed() {
        setPassword.setUsed(true);
        when(setPasswordRepository.findByToken(token)).thenReturn(Optional.of(setPassword));
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken(token);
        request.setPassword("newPassWord");
        assertThrows(ResponseStatusException.class, () -> setPasswordService.setPassword(request));
    }

    @Test
    void shouldSetPasswordForCustomer() {
        Customer customer = new Customer();
        when(setPasswordRepository.findByToken(token)).thenReturn(Optional.of(setPassword));
        when(customerRepository.findById(userId)).thenReturn(Optional.of(customer));
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken(token);
        request.setPassword("newPassWord");

        setPasswordService.setPassword(request);

        assertNotNull(customer.getPassword());
        assertNotNull(customer.getSaltPassword());
        assertTrue(setPassword.getUsed());
        verify(customerRepository, times(1)).save(customer);
        verify(setPasswordRepository, times(1)).save(setPassword);
    }

    @Test
    void shouldSetPasswordForEmployee() {
        setPassword.setCustomer(false);
        Employee employee = new Employee();
        when(setPasswordRepository.findByToken(token)).thenReturn(Optional.of(setPassword));
        when(employeeRepository.findById(userId)).thenReturn(Optional.of(employee));
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken(token);
        request.setPassword("newPassWord");

        setPasswordService.setPassword(request);

        assertNotNull(employee.getPassword());
        assertNotNull(employee.getSaltPassword());
        assertTrue(setPassword.getUsed());
        verify(employeeRepository, times(1)).save(employee);
        verify(setPasswordRepository, times(1)).save(setPassword);
    }
}