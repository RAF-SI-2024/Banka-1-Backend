package com.banka1.user;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.security.JwtUtil;
import com.banka1.user.service.AuthService;
import com.banka1.user.service.BlackListTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private BlackListTokenService blackListTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private com.banka1.user.controllers.AuthController authController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    // Uspesan login(status 200 i validan token)
    @Test
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn("mocked_jwt_token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mocked_jwt_token"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // Neuspesan login(status 400 i ispravna poruka greske)
    @Test
    void testLoginUserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("Korisnik ne postoji."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Korisnik ne postoji."));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // Uspesan logout(status 200 i potvrda odjave)
    @Test
    void testLogoutSuccess() throws Exception {
        String token = "validToken";

        doNothing().when(blackListTokenService).blacklistToken(token);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Korisnik odjavljen"));

        verify(blackListTokenService, times(1)).blacklistToken(token);
    }

    // Logout bez tokena(status 400 i ispravna poruka greske)
    @Test
    void testLogoutWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Token nije prosleđen ili je neispravan."));
    }

    // Uspesan refresh tokena(status 200 i novi token)
    @Test
    void testRefreshTokenSuccess() throws Exception {
        String oldToken = "validOldToken";
        String newToken = "newValidToken";

        Claims claims = new DefaultClaims();
        claims.put("userId", 1L);
        claims.put("role", "CUSTOMER");
        claims.put("permissions", List.of("READ_EMPLOYEE"));

        when(blackListTokenService.isTokenBlacklisted(oldToken)).thenReturn(false);
        when(jwtUtil.validateToken(oldToken)).thenReturn(true);
        when(jwtUtil.getClaimsFromToken(oldToken)).thenReturn(claims);
        when(jwtUtil.generateToken(1L, "CUSTOMER", List.of("READ_EMPLOYEE"))).thenReturn(newToken);

        mockMvc.perform(get("/api/auth/refresh-token")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(newToken));

        verify(blackListTokenService, times(1)).blacklistToken(oldToken);
        verify(jwtUtil, times(1)).generateToken(1L, "CUSTOMER", List.of("READ_EMPLOYEE"));
    }

    // Refresh token sa blacklistovanim tokenom(status 403)
    @Test
    void testRefreshTokenWithBlacklistedToken() throws Exception {
        String oldToken = "blacklistedToken";

        when(blackListTokenService.isTokenBlacklisted(oldToken)).thenReturn(true);

        mockMvc.perform(get("/api/auth/refresh-token")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Token je već istekao ili je nevažeći."));
    }

    // Refresh token sa nevalidnim tokenom(status 403)
    @Test
    void testRefreshTokenWithInvalidToken() throws Exception {
        String oldToken = "invalidToken";

        when(blackListTokenService.isTokenBlacklisted(oldToken)).thenReturn(false);
        when(jwtUtil.validateToken(oldToken)).thenReturn(false);

        mockMvc.perform(get("/api/auth/refresh-token")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Token nije validan"));
    }
}
