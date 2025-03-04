package com.banka1.user.controllers;


import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.model.helper.Position;
import com.banka1.user.service.BlackListTokenService;
import com.banka1.user.service.IAuthService;
import com.banka1.user.utils.ResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Mock
    private IAuthService authService;
    @Mock
    private BlackListTokenService blackListTokenService;
    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    // Uspesan login(status 200 i validan token)
    @Test
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");

        when(authService.login(loginRequest)).thenReturn("mocked_jwt_token");

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

        when(authService.login(any(LoginRequest.class))).thenThrow(new IllegalArgumentException("Korisnik ne postoji."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(ResponseMessage.INVALID_USER.toString()));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // Uspesan logout(status 200 i potvrda odjave)
    @Test
    void testLogoutSuccess() throws Exception {
        String token = "validToken";

        when(authService.getToken(anyString())).thenReturn(token);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value(ResponseMessage.LOGOUT_SUCCESS.toString()));

        verify(blackListTokenService, times(1)).blacklistToken(token);
    }

    // Logout bez tokena(status 401)
    @Test
    void testLogoutWithoutToken() throws Exception {
        when(authService.getToken(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // Uspesan refresh tokena(status 200 i novi token)
    @Test
    void testRefreshTokenSuccess() throws Exception {
        String oldToken = "validOldToken";
        String newToken = "newValidToken";

        Claims claims = new DefaultClaims(Map.of(
            "id", 1L,
            "position", Position.NONE.toString(),
            "permissions", List.of(Permission.READ_EMPLOYEE.toString())
        ));

        when(authService.getToken(anyString())).thenReturn(oldToken);
        when(authService.recreateToken(oldToken)).thenReturn(newToken);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(newToken));

        verify(blackListTokenService, times(1)).blacklistToken(oldToken);
    }

    // Refresh token sa nevalidnim tokenom(status 401)
    @Test
    void testRefreshTokenWithInvalidToken() throws Exception {
        String oldToken = "invalidToken";

        mockMvc.perform(post("/api/auth/refresh-token")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
