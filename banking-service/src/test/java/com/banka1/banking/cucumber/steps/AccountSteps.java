package com.banka1.banking.cucumber.steps;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class AccountSteps {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String token;
    private ResponseEntity<Map> responseEntity;
    private HttpStatusCodeException exception;
    private Map<String, Object> accountData;

    @Before
    public void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        exception = null;
        token = null;
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @SuppressWarnings({ "unchecked" })
    @Given("employee is logged into the account portal")
    public void employeeIsLoggedIntoAccountPortal() {
        // Login with employee credentials to get a JWT token
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", "admin@admin.com");
        loginData.put("password", "admin123");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8081/api/auth/login", loginData, Map.class);

            token = (String) ((Map<String, Object>) response.getBody().get("data")).get("token");
            assertNotNull(token, "Token should be generated during employee login");
            System.out.println("Employee authenticated with token length: " + token.length());
        } catch (RestClientException e) {
            fail("Login failed: " + e.getMessage());
        }
    }

    @When("employee navigates to account creation portal")
    public void employeeNavigatesToAccountCreationPortal() {
        // This is a UI action that we're simulating for API testing
        System.out.println("Employee navigates to account creation portal (simulated)");
    }

    @And("employee enters the account information")
    public void employeeEntersTheAccountInformation() {
        accountData = new HashMap<>();
        accountData.put("ownerID", 1);
        accountData.put("currency", "RSD");
        accountData.put("type", "CURRENT");
        accountData.put("subtype", "STANDARD");
        accountData.put("dailyLimit", 0);
        accountData.put("monthlyLimit", 0);
        accountData.put("status", "ACTIVE");
        accountData.put("createCard", false);
        accountData.put("balance", 200000);

        System.out.println("Account information filled with owner ID: " + accountData.get("ownerID") +
                " and type: " + accountData.get("type"));
    }

    @And("employee does not enter the account information")
    public void employeeDoesNotEnterTheAccountInformation() {
        // Create an incomplete account data object for failure case
        accountData = new HashMap<>();
        accountData.put("ownerID", 1);
        // Missing required fields

        System.out.println("Incomplete account information with only owner ID: " + accountData.get("ownerID"));
    }

    @And("employee presses the Create button for account")
    public void employeePressesTheCreateButtonForAccount() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(accountData, headers);

        try {
            System.out.println("Sending account creation request with data: " + accountData);
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/accounts/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            System.out.println("Account creation response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error creating account: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @Then("the account should be succesfully created")
    public void theAccountShouldBeSuccesfullyCreated() {
        assertNull(exception, "No exception should be thrown during account creation");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode(), "Expected 201 CREATED status");

        // Verify the success flag in the response
        Map<String, Object> responseBody = responseEntity.getBody();
        assertTrue((Boolean) responseBody.get("success"), "Response should indicate success");
    }

    @Then("the account should not be succesfully created")
    public void theAccountShouldNotBeSuccesfullyCreated() {
        assertNotNull(exception, "Exception should be thrown for invalid account creation");
        assertTrue(exception.getStatusCode().is4xxClientError(),
                "Should return a client error status code");

        String errorResponse = exception.getResponseBodyAsString();
        System.out.println("Received expected error during account creation: " + errorResponse);
    }
}