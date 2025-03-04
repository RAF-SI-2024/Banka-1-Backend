package com.banka1.user.services;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.aspect.AuthAspect;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.model.helper.Position;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.service.BlackListTokenService;
import com.banka1.user.service.IAuthService;
import com.banka1.user.service.implementation.AuthService;
import com.banka1.user.utils.ResponseMessage;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private IAuthService mockAuthService;
    @InjectMocks
    private AuthService authService;
    @Mock
    private BlackListTokenService blackListTokenService;
    @Mock
    private Claims claims;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private MethodSignature methodSignature;
    @Mock
    private Method method;
    @Mock
    private Authorization authorization;
    @InjectMocks
    private AuthAspect authAspect;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "secret", Base64.getEncoder().encodeToString("TEST_SECRET_12345678_ABC!_00defgh".getBytes()));
        ReflectionTestUtils.setField(authService, "jwtExpiration", 100000);
    }

    @Test
    void authorizationTest_invalidToken() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer NevalidanToken" });

        authAspect.authorize(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_blankToken() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { null });

        authAspect.authorize(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenNoPerms() throws Throwable {
        when(claims.get("permissions")).thenReturn(List.of(Permission.CREATE_EMPLOYEE.toString()));
        when(claims.get("isAdmin", Boolean.class)).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken" });
        when(mockAuthService.parseToken(notNull(String.class))).thenReturn(claims);
        when(authorization.permissions()).thenReturn(new Permission[] { Permission.READ_EMPLOYEE });
        when(authorization.positions()).thenReturn(new Position[] { });
        when(method.getAnnotation(Authorization.class)).thenReturn(authorization);
        when(methodSignature.getMethod()).thenReturn(method);
        when(mockAuthService.getToken(notNull(String.class))).thenReturn("ValidanToken");
        when(blackListTokenService.isTokenBlacklisted(notNull(String.class))).thenReturn(false);

        authAspect.authorize(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenWrongPosition() throws Throwable {
        when(claims.get("permissions")).thenReturn(List.of());
        when(claims.get("position", String.class)).thenReturn(Position.WORKER.toString());
        when(claims.get("isAdmin", Boolean.class)).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken" });
        when(mockAuthService.parseToken(notNull(String.class))).thenReturn(claims);
        when(authorization.permissions()).thenReturn(new Permission[] { });
        when(authorization.positions()).thenReturn(new Position[] { Position.MANAGER, Position.DIRECTOR });
        when(method.getAnnotation(Authorization.class)).thenReturn(authorization);
        when(methodSignature.getMethod()).thenReturn(method);
        when(mockAuthService.getToken(notNull(String.class))).thenReturn("ValidanToken");
        when(blackListTokenService.isTokenBlacklisted(notNull(String.class))).thenReturn(false);

        authAspect.authorize(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenWithInvalidIdFallback() throws Throwable {
        when(claims.get("permissions")).thenReturn(List.of(Permission.CREATE_EMPLOYEE.toString()));
        when(claims.get("id", Long.class)).thenReturn(1L);
        when(claims.get("isAdmin", Boolean.class)).thenReturn(false);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "id" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 2L });
        when(mockAuthService.parseToken(notNull(String.class))).thenReturn(claims);
        when(authorization.permissions()).thenReturn(new Permission[] { Permission.READ_EMPLOYEE });
        when(authorization.positions()).thenReturn(new Position[] { });
        when(authorization.allowIdFallback()).thenReturn(true);
        when(method.getAnnotation(Authorization.class)).thenReturn(authorization);
        when(methodSignature.getMethod()).thenReturn(method);
        when(mockAuthService.getToken(notNull(String.class))).thenReturn("ValidanToken");
        when(blackListTokenService.isTokenBlacklisted(notNull(String.class))).thenReturn(false);

        authAspect.authorize(joinPoint);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void authorizationTest_validTokenWithValidPermsAndPositions() throws Throwable {
        when(claims.get("permissions")).thenReturn(List.of(Permission.CREATE_EMPLOYEE.toString(), Permission.READ_EMPLOYEE.toString()));
        when(claims.get("position", String.class)).thenReturn(Position.MANAGER.toString());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken" });
        when(mockAuthService.parseToken(notNull(String.class))).thenReturn(claims);
        when(authorization.permissions()).thenReturn(new Permission[] { Permission.CREATE_EMPLOYEE });
        when(authorization.positions()).thenReturn(new Position[] { Position.MANAGER, Position.DIRECTOR });
        when(method.getAnnotation(Authorization.class)).thenReturn(authorization);
        when(methodSignature.getMethod()).thenReturn(method);
        when(mockAuthService.getToken(notNull(String.class))).thenReturn("ValidanToken");
        when(blackListTokenService.isTokenBlacklisted(notNull(String.class))).thenReturn(false);

        authAspect.authorize(joinPoint);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void authorizationTest_validTokenWithValidIdFallback() throws Throwable {
        when(claims.get("permissions")).thenReturn(List.of(Permission.CREATE_EMPLOYEE.toString()));
        when(claims.get("id", Long.class)).thenReturn(1L);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[] { "authorization", "id" });
        when(joinPoint.getArgs()).thenReturn(new Object[] { "Bearer ValidanToken", 1L });
        when(mockAuthService.parseToken(notNull(String.class))).thenReturn(claims);
        when(authorization.permissions()).thenReturn(new Permission[] { Permission.READ_EMPLOYEE });
        when(authorization.positions()).thenReturn(new Position[] { });
        when(authorization.allowIdFallback()).thenReturn(true);
        when(method.getAnnotation(Authorization.class)).thenReturn(authorization);
        when(methodSignature.getMethod()).thenReturn(method);
        when(mockAuthService.getToken(notNull(String.class))).thenReturn("ValidanToken");
        when(blackListTokenService.isTokenBlacklisted(notNull(String.class))).thenReturn(false);

        authAspect.authorize(joinPoint);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void authenticationTest_validUserLogin() {
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

        String token = authService.login(loginRequest);
        assertNotNull(token);

        verify(customerRepository, times(1)).findByEmail(anyString());
    }

    @Test
    void authenticationTest_invalidUserLogin() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexist@example.com");
        loginRequest.setPassword("password123");

        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(loginRequest));

        assertEquals(ResponseMessage.INVALID_USER.toString(), exception.getMessage());
    }

    @Test
    void authenticationTest_validTokenGenerated() {
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

        String token = assertDoesNotThrow(() -> authService.login(loginRequest));

        Claims claims = authService.parseToken(token);
        assertNotNull(claims);

        assertEquals(customer.getId(), claims.get("id", Long.class));
        assertEquals(Position.NONE.toString(), claims.get("position", String.class));
        List<String> permissionsFromToken = (List<String>) claims.get("permissions");
        assertNotNull(permissionsFromToken);
        assertEquals(customer.getPermissions().stream().map(Permission::toString).collect(Collectors.toList()), permissionsFromToken);
    }
}
