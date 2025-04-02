package com.banka1.user;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.service.BlackListTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Autowired
    private BlackListTokenService blacklistTokenService;

    @BeforeEach
    void setup() {
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @AfterEach
    void tearDown() {
        blacklistTokenService.clear();
    }


    @Test
    void testEmployee() {
        String id = "1";

        var token = loginAsAdmin();

        var responseEntity = sendGetRequest(token, "/users/employees/" + id);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        var data = (Map) responseEntity.getBody().get("data");
        var employee = data;

        var requiredFields = List.of("firstName", "lastName", "email", "phoneNumber", "position", "active");

        for (var field : requiredFields)
            assertTrue(data.containsKey(field), "Podacima nedostaje polje " + field);

        assertEquals(id, data.get("id").toString(), "Pogresan ID.");

        responseEntity = sendGetRequest(token, "/users/search/employees");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertTrue(1 <= (Integer)data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        var rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga nisu vratili isti objekat.");

        var filterValue = "dmi";

        responseEntity = sendGetRequest(token, "/users/search/employees?filterField=firstName&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(1, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom nisu vratili isti objekat.");

        responseEntity = sendGetRequest(token, "/users/search/employees?filterField=firstName&sortField=firstName&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(1, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom + sortiranjem nisu vratili isti objekat.");

        filterValue = "director";

        responseEntity = sendGetRequest(token, "/users/search/employees?filterField=position&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(1, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom po poziciji nisu vratili isti objekat.");
    }

    @Test
    void testCustomer() {
        String id = "1";

        var token = loginAsAdmin();

        var responseEntity = sendGetRequest(token, "/customer/" + id);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        var data = (Map) responseEntity.getBody().get("data");
        var employee = data;

        var requiredFields = List.of("firstName", "lastName", "email", "phoneNumber");

        for (var field : requiredFields)
            assertTrue(data.containsKey(field), "Podacima nedostaje polje " + field);

        assertEquals(id, data.get("id").toString(), "Pogresan ID.");

        responseEntity = sendGetRequest(token, "/users/search/customers");

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(8, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        var rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga nisu vratili isti objekat.");

        var filterValue = "ark";

        responseEntity = sendGetRequest(token, "/users/search/customers?filterField=firstName&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(1, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom nisu vratili isti objekat.");

        responseEntity = sendGetRequest(token, "/users/search/customers?filterField=firstName&sortField=firstName&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        assertEquals(1, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom + sortiranjem nisu vratili isti objekat.");

        filterValue = "male";

        responseEntity = sendGetRequest(token, "/users/search/customers?filterField=gender&filterValue=" + filterValue);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "GET nije uspešan.");
        assertNotNull(responseEntity.getBody(), "Odgovor je null.");
        assertNotNull(responseEntity.getBody().get("data"), "Odgovor ne sadrži data.");
        assertInstanceOf(Map.class, responseEntity.getBody().get("data"), "Podaci nisu mapa.");

        data = (Map) responseEntity.getBody().get("data");

        assertNotNull(data.get("total"), "Odgovor ne sadrži broj redova.");
        System.out.println(data);
        assertEquals(5, data.get("total"));
        assertNotNull(data.get("rows"), "Odgovor ne sadrži redove.");
        assertInstanceOf(List.class, data.get("rows"), "Redovi nisu lista.");

        rows = (List) data.get("rows");

        assertEquals(rows.get(0), employee, "Pristup preko ID-a i pretraga sa filterom po polu nisu vratili isti objekat.");
    }

    private ResponseEntity<Map> sendGetRequest(String token, String path) {
        var headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        var request = new HttpEntity<>(headers);
        var responseEntity = restTemplate.exchange(getBaseUrl() + path, HttpMethod.GET, request, Map.class);
        return responseEntity;
    }

    private String loginAsAdmin() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@admin.com");
        loginRequest.setPassword("admin123");

        @SuppressWarnings("rawtypes") ResponseEntity<Map> loginResponse = restTemplate.postForEntity(getBaseUrl() + "/auth/login", loginRequest, Map.class);

        assertNotNull(loginResponse.getBody());
        @SuppressWarnings("unchecked")
        var token = (String) ((Map<String, Object>) loginResponse.getBody().get("data")).get("token");
        return token;
    }
}
