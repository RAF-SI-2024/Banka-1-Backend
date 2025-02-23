package com.banka1.user;

import com.banka1.user.DTO.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // Koristi test profil ako postoji
public class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @Test
    void testLoginAndLogoutFlow() {
        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@admin.com");
        loginRequest.setPassword("admin");  // Podaci iz BootstrapData

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                getBaseUrl() + "/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), "Login nije uspešan.");
        assertNotNull(loginResponse.getBody().get("data"), "Login odgovor ne sadrži data.");

        String token = (String) ((Map<String, Object>) loginResponse.getBody().get("data")).get("token");
        assertNotNull(token, "Token nije generisan tokom login-a.");

        // Logout
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> logoutResponse = restTemplate.exchange(
                getBaseUrl() + "/logout", HttpMethod.POST, logoutRequest, Map.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode(), "Logout nije uspešan.");
        assertEquals("Korisnik odjavljen",
                ((Map<String, Object>) logoutResponse.getBody().get("data")).get("message"),
                "Poruka nakon logout-a nije ispravna.");
    }

    // Ovaj test ne treba da prodje jer se nakon logout-a korisnika pokusava pristup ruti
    // namenjenoj samo za testiranje funkcionalnosti validnosti tokena nakon logout-a
    @Test
    void testLoginAndLogoutWithBlacklistCheck() {
        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@admin.com");
        loginRequest.setPassword("admin");

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                getBaseUrl() + "/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String token = (String) ((Map<String, Object>) loginResponse.getBody().get("data")).get("token");
        assertNotNull(token);

        // Logout
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> logoutResponse = restTemplate.exchange(
                getBaseUrl() + "/logout", HttpMethod.POST, logoutRequest, Map.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertEquals("Korisnik odjavljen",
                ((Map<String, Object>) logoutResponse.getBody().get("data")).get("message"));

        // Pokušaj da koristiš isti token nakon logout-a
        HttpEntity<Void> protectedRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> protectedResponse = restTemplate.exchange(
                getBaseUrl() + "/protected",  // Zameniti stvarnom zaštićenom rutom
                HttpMethod.GET,
                protectedRequest,
                Map.class);

        // Provera da je token odbijen (jer je blacklistovan)
        assertEquals(HttpStatus.FORBIDDEN, protectedResponse.getStatusCode());
        assertEquals("Token je već istekao ili je nevažeći.",
                protectedResponse.getBody().get("error"));
    }


}

