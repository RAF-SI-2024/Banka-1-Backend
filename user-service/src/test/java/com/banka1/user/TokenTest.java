package com.banka1.user;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.security.JwtUtil;
import com.banka1.user.service.AuthService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class TokenTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(customerRepository, employeeRepository, jwtUtil);
    }

    @Test
    void testLoginWithValidCustomerCredentialsAndGenerateToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("customer@example.com");
        loginRequest.setPassword("password123");

        String salt = "salt";
        String hashedPassword = BCrypt.hashpw("password123" + salt, BCrypt.gensalt());

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setPassword(hashedPassword);
        customer.setSaltPassword(salt);
        customer.setPermissions(List.of(
                Permission.READ_EMPLOYEE,
                Permission.CREATE_EMPLOYEE
        ));

        when(customerRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(employeeRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        String token = authService.login(loginRequest);

        assertNotNull(token, "Token ne sme biti null!");
        assertTrue(jwtUtil.validateToken(token), "Token nije validan!");

        Claims claims = jwtUtil.getClaimsFromToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("CUSTOMER", claims.get("role", String.class));

        @SuppressWarnings("unchecked")
        List<String> permissionsFromToken = (List<String>) claims.get("permissions");
        assertNotNull(permissionsFromToken);
        assertEquals(List.of("READ_EMPLOYEE", "CREATE_EMPLOYEE"), permissionsFromToken);

        System.out.println("Generisani token: " + token);
    }

}

