package com.banka1.user.cucumber;

import com.banka1.user.DTO.request.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.java.Before;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SearchCucumberTest {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private String token;

    private HttpEntity<Void> request;

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> responseEntity;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/users/search";
    }

    @Before
    void setup() {
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @SuppressWarnings("unchecked")
    @Given("an administrator is logged in")
    public void an_administrator_is_logged_in() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@admin.com");
        loginRequest.setPassword("admin123");

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/auth/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), "Login nije uspešan.");
        assertNotNull(loginResponse.getBody().get("data"), "Login odgovor ne sadrži data.");

        token = (String) ((Map<String, Object>) loginResponse.getBody().get("data")).get("token");
        assertNotNull(token, "Token nije generisan tokom login-a.");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        request = new HttpEntity<>(headers);
    }

    @When("one {word} search query is sent for {string} containing {string}")
    public void one_search_query_is_sent_for_containing(String type, String field, String filter) {
        boolean noBlanks = field.length() > 0 || filter.length() > 0;
        responseEntity = restTemplate.exchange(getBaseUrl() + "/" + type + (noBlanks ? "?filterField=" + field + "&filterValue=" + filter : ""), HttpMethod.GET, request, Map.class);
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    }

    @Then("it's {word} that results are returned")
    public void its_that_results_are_returned(String val) {
        boolean found = Boolean.parseBoolean(val);
        assertTrue((found && Integer.parseInt(((Map<?, ?>) responseEntity.getBody().get("data")).get("total").toString()) > 0) || (!found && Integer.parseInt(((Map<?, ?>) responseEntity.getBody().get("data")).get("total").toString()) == 0));
    }
}
