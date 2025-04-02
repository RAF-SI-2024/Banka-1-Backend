package com.banka1.user.services;

import com.banka1.user.DTO.request.NotificationRequest;
import com.banka1.user.DTO.request.ResetPasswordConfirmationRequest;
import com.banka1.user.DTO.request.ResetPasswordRequest;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.model.ResetPassword;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.repository.ResetPasswordRepository;
import com.banka1.user.service.ResetPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @InjectMocks
    private ResetPasswordService resetPasswordService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ResetPasswordRepository resetPasswordRepository;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessageHelper messageHelper;

    @Captor
    private ArgumentCaptor<ResetPassword> resetPasswordCaptor;

    private final String testEmail = "test@example.com";
    private final String destinationEmail = "queue.email";

    @BeforeEach
    void setup() {
        resetPasswordService = new ResetPasswordService(
                customerRepository, resetPasswordRepository, employeeRepository,
                jmsTemplate, messageHelper, destinationEmail
        );
    }

    @Test
    void request_password_reset_should_create_reset_token_for_customer() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(testEmail);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setEmail(testEmail);

        when(customerRepository.findByEmail(testEmail)).thenReturn(Optional.of(customer));
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        resetPasswordService.requestPasswordReset(request);

        verify(resetPasswordRepository).save(resetPasswordCaptor.capture());
        ResetPassword savedResetPassword = resetPasswordCaptor.getValue();

        assertNotNull(savedResetPassword.getToken());
        assertEquals(customer.getId(), savedResetPassword.getUserId());
        assertFalse(savedResetPassword.getUsed());
        assertEquals(0, savedResetPassword.getType());


    }

    @Test
    void request_password_reset_should_create_reset_token_for_employee() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(testEmail);

        Employee employee = new Employee();
        employee.setId(2L);
        employee.setEmail(testEmail);

        when(customerRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.of(employee));

        resetPasswordService.requestPasswordReset(request);

        verify(resetPasswordRepository).save(resetPasswordCaptor.capture());
        ResetPassword savedResetPassword = resetPasswordCaptor.getValue();

        assertNotNull(savedResetPassword.getToken());
        assertEquals(employee.getId(), savedResetPassword.getUserId());
        assertFalse(savedResetPassword.getUsed());
        assertEquals(1, savedResetPassword.getType());


    }

    @Test
    void request_password_reset_should_throw_exception_when_email_not_found() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(testEmail);

        when(customerRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                resetPasswordService.requestPasswordReset(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Email adresa nije pronadjena.", exception.getReason());
    }

    @Test
    void reset_password_should_update_customer_password() {
        String token = UUID.randomUUID().toString();
        ResetPasswordConfirmationRequest request = new ResetPasswordConfirmationRequest();
        request.setToken(token);
        request.setPassword("newSecurePassword");

        ResetPassword resetPassword = new ResetPassword();
        resetPassword.setToken(token);
        resetPassword.setUsed(false);
        resetPassword.setExpirationDate(System.currentTimeMillis() + 1000 * 60);
        resetPassword.setUserId(1L);
        resetPassword.setType(0);

        Customer customer = new Customer();
        customer.setId(1L);

        when(resetPasswordRepository.findByToken(token)).thenReturn(resetPassword);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        resetPasswordService.resetPassword(request);

        verify(customerRepository).save(customer);
        assertTrue(resetPassword.getUsed());
        verify(resetPasswordRepository).save(resetPassword);
    }

    @Test
    void reset_password_should_update_employee_password() {
        String token = UUID.randomUUID().toString();
        ResetPasswordConfirmationRequest request = new ResetPasswordConfirmationRequest();
        request.setToken(token);
        request.setPassword("newSecurePassword");

        ResetPassword resetPassword = new ResetPassword();
        resetPassword.setToken(token);
        resetPassword.setUsed(false);
        resetPassword.setExpirationDate(System.currentTimeMillis() + 1000 * 60);
        resetPassword.setUserId(2L);
        resetPassword.setType(1);

        Employee employee = new Employee();
        employee.setId(2L);

        when(resetPasswordRepository.findByToken(token)).thenReturn(resetPassword);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        resetPasswordService.resetPassword(request);

        verify(employeeRepository).save(employee);
        assertTrue(resetPassword.getUsed());
        verify(resetPasswordRepository).save(resetPassword);
    }

    @Test
    void reset_password_should_throw_exception_when_token_not_found() {
        ResetPasswordConfirmationRequest request = new ResetPasswordConfirmationRequest();
        request.setToken("invalid-token");
        request.setPassword("password");

        when(resetPasswordRepository.findByToken("invalid-token")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                resetPasswordService.resetPassword(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Token nije pronadjen.", exception.getReason());
    }

    @Test
    void reset_password_should_throw_exception_when_token_expired() {
        ResetPasswordConfirmationRequest request = new ResetPasswordConfirmationRequest();
        request.setToken("expired-token");
        request.setPassword("password");

        ResetPassword resetPassword = new ResetPassword();
        resetPassword.setToken("expired-token");
        resetPassword.setUsed(false);
        resetPassword.setExpirationDate(System.currentTimeMillis() - 1000);
        resetPassword.setUserId(1L);
        resetPassword.setType(0);

        when(resetPasswordRepository.findByToken("expired-token")).thenReturn(resetPassword);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                resetPasswordService.resetPassword(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Token je istekao.", exception.getReason());
    }

    @Test
    void reset_password_should_throw_exception_when_token_already_used() {
        ResetPasswordConfirmationRequest request = new ResetPasswordConfirmationRequest();
        request.setToken("used-token");
        request.setPassword("password");

        ResetPassword resetPassword = new ResetPassword();
        resetPassword.setToken("used-token");
        resetPassword.setUsed(true);
        resetPassword.setExpirationDate(System.currentTimeMillis() + 1000);
        resetPassword.setUserId(1L);
        resetPassword.setType(0);

        when(resetPasswordRepository.findByToken("used-token")).thenReturn(resetPassword);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                resetPasswordService.resetPassword(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Token je već iskorišćen.", exception.getReason());
    }
}