package com.banka1.user.cucumber;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.java.After;
import io.cucumber.java.Before;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuthCucumberTest {
    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private String token;

    @Autowired
    private EmployeeRepository employeeRepository;

    private ResponseEntity<?> responseEntity;

    private HttpEntity<Void> request;

    private HttpStatusCodeException exception;


    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    private String getEmployeeUrl() {
        return "http://localhost:" + port + "/api/users/employees";
    }

    private String getCustomerUrl() {
        return "http://localhost:" + port + "/api/customer";
    }

    @Before
    void setup() {
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @Given("{word} \\({word}\\) is an employee")
    public void is_an_employee(String employeeName, String employeeEmail) {
        assertTrue(employeeRepository.existsByEmail(employeeEmail));
    }

    @SuppressWarnings("unchecked")
    @When("{word} logs in with the valid credentials {word} and {word}")
    public void logs_in_with_the_valid_credentials_and(String name, String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(getAuthUrl() + "/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), "Login nije uspešan.");
        assertNotNull(loginResponse.getBody().get("data"), "Login odgovor ne sadrži data.");

        token = (String) ((Map<String, Object>) loginResponse.getBody().get("data")).get("token");
        assertNotNull(token, "Token nije generisan tokom login-a.");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        request = new HttpEntity<>(headers);
    }

    @And("{word} is not logged in")
    public void is_not_logged_in(String name) {
        assertNull(token);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "");

        request = new HttpEntity<>(headers);
    }

    @When("{word} tries to view the details of the employee with the ID {long}")
    public void tries_to_view_the_details_of_the_employee_with_the_id(String name, Long id) {
        try {
            responseEntity = restTemplate.exchange(getEmployeeUrl() + "/" + id, HttpMethod.GET, request, Map.class);
        } catch (HttpStatusCodeException e) {
            exception = e;
        }
    }

    @When("{word} tries to view the details of the customer with the ID {long}")
    public void tries_to_view_the_details_of_the_customer_with_the_id(String name, Long id) {
        try {
            responseEntity = restTemplate.exchange(getCustomerUrl() + "/" + id, HttpMethod.GET, request, Map.class);
        } catch (HttpStatusCodeException e) {
            exception = e;
        }
    }

    @Then("the request is authorized")
    public void the_request_is_authorized() {
        assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    }

    @Then("the request is not authorized")
    public void the_request_is_not_authorized() {
        assertTrue(exception.getStatusCode().is4xxClientError());
    }
}
