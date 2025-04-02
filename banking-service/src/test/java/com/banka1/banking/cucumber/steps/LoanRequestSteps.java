package com.banka1.banking.cucumber.steps;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.springframework.beans.factory.annotation.Autowired;
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

public class LoanRequestSteps {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private String token;
    private ResponseEntity<?> responseEntity;
    private HttpStatusCodeException exception;
    private Map<String, Object> loanRequestData;

    @Before
    public void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        exception = null;
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @SuppressWarnings({ "unchecked", "null" })
    @Given("customer is logged into the banking portal")
    public void customerIsLoggedIntoBankingPortal() {
        // Simulating a customer login and getting a token
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", "jpavlovic6521rn@raf.rs");
        loginData.put("password", "Jov@njovan1");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8081/api/auth/login", loginData, Map.class);

            token = (String) ((Map<String, Object>) response.getBody().get("data")).get("token");
            assertNotNull(token, "Token should be generated during login");
        } catch (RestClientException e) {
            fail("Login failed: " + e.getMessage());
        }
    }

    @And("customer navigates to the loans portal")
    public void customerNavigatesToLoansPortal() {
        // This is a UI action that we're simulating for API testing
        // No actual API call needed for this step
    }

    @When("customer presses the Apply for Loan button")
    public void customerPressesApplyForLoanButton() {
        // Initialize loan request data to prepare for form filling
        loanRequestData = new HashMap<>();
    }

    @And("customer fills out the loan request form")
    public void customerFillsOutLoanRequestForm() {
        loanRequestData.put("accountId", 100);
        loanRequestData.put("currencyType", "RSD");
        loanRequestData.put("employmentDuration", 12);
        loanRequestData.put("employmentStatus", "PERMANENT");
        loanRequestData.put("interestType", "FIXED");
        loanRequestData.put("loanAmount", 12000);
        loanRequestData.put("loanPurpose", "Cocaine");
        loanRequestData.put("loanType", "CASH");
        loanRequestData.put("numberOfInstallments", 24);
        loanRequestData.put("phoneNumber", "063457732");
        loanRequestData.put("salaryAmount", 89000);
    }

    @And("customer fills out the loan request form with missing required information")
    public void customerFillsOutLoanRequestFormWithMissingInfo() {
        loanRequestData.put("accountId", 100);
        loanRequestData.put("currencyType", "RSD");
        loanRequestData.put("employmentDuration", 12);
        loanRequestData.put("employmentStatus", "PERMANENT");
        loanRequestData.put("interestType", "FIXED");
        loanRequestData.put("loanType", "CASH");
        loanRequestData.put("numberOfInstallments", 24);
        loanRequestData.put("salaryAmount", 89000);
    }

    @And("customer submits the loan request form")
    public void customerSubmitsLoanRequestForm() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(loanRequestData, headers);

        try {
            // Add debugging to see what's being sent
            System.out.println("Sending loan request with token: " + token.substring(0, 10) + "...");
            System.out.println("Request data: " + loanRequestData);

            // Use the correct API endpoint - add "/api" prefix and remove trailing slash
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/loans/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // Print response for debugging
            System.out.println("Response status: " + responseEntity.getStatusCode());
            System.out.println("Response body: " + responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error status: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("null")
    @Then("the loan request should be successfully submitted")
    public void loanRequestShouldBeSuccessfullySubmitted() {
        assertNull(exception, "No exception should be thrown");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) responseEntity.getBody();
        assertTrue((Boolean) responseBody.get("success"), "Response should indicate success");
    }

    @Then("the loan request should not be submitted")
    public void loanRequestShouldNotBeSubmitted() {
        assertNotNull(exception, "Exception should be thrown for invalid request");
        assertTrue(exception.getStatusCode().is4xxClientError(),
                "Should return a client error status code");
    }

    @When("customer presses the Details button")
    public void customerPressesDetailsButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            // Use consistent API path structure
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/loans/1",
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            System.out.println("Details response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Details error: " + e.getResponseBodyAsString());
        }
    }

}