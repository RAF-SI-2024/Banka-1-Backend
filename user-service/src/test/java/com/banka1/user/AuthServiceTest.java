package com.banka1.user;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.Customer;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.security.JwtUtil;
import com.banka1.user.service.AuthService;
import com.banka1.user.service.BlackListTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(customerRepository,employeeRepository,jwtUtil);
    }

    // Uspesan login sa validnim korisnickim podacima
    @Test
    void testLoginWithValidCustomerCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("customer@example.com");
        loginRequest.setPassword("password123");

        String salt = "salt";
        String hashedPassword = BCrypt.hashpw("password123" + salt, BCrypt.gensalt());

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPassword(hashedPassword);
        customer.setSaltPassword(salt);
        customer.setPermissions(List.of());

        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));
        when(employeeRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(eq(1L), eq("CUSTOMER"), anyList())).thenReturn("mocked_jwt_token");

        String token = authService.login(loginRequest);

        assertNotNull(token);
        assertEquals("mocked_jwt_token", token);

        verify(customerRepository, times(1)).findByEmail(anyString());
        verify(jwtUtil, times(1)).generateToken(eq(1L), eq("CUSTOMER"), anyList());
    }

    // Login sa nepostojecim korisnikom
    @Test
    void testLoginWithNonExistingUserThrowsException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexist@example.com");
        loginRequest.setPassword("password123");

        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals("Korisnik ne postoji.", exception.getMessage());
    }

}
