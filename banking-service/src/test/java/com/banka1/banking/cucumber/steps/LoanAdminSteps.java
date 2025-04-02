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

public class LoanAdminSteps {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private String token;
    private ResponseEntity<Map> responseEntity;
    private HttpStatusCodeException exception;
    private final long loanIdToApprove = 2;
    private final long loanIdToDeny = 1;

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
    @Given("employee is logged into the banking portal")
    public void employeeIsLoggedIntoBankingPortal() {
        // Login with employee credentials to get a JWT token
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", "admin@admin.com");
        loginData.put("password", "admin123");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8081/api/auth/login", loginData, Map.class);

            System.out.println("Login response: " + response.getStatusCode());

            token = (String) ((Map<String, Object>) response.getBody().get("data")).get("token");
            assertNotNull(token, "Token should be generated during employee login");
            System.out.println("Employee authenticated with token: " + token.substring(0, 10) + "...");
        } catch (RestClientException e) {
            fail("Login failed: " + e.getMessage());
        }
    }

    @And("employee navigates to the pending loans portal")
    public void employeeNavigatesToPendingLoansPortal() {
        // This is a UI action that we're simulating for API testing
        // No actual API call needed for this step
        System.out.println("Employee navigates to pending loans portal (simulated)");
    }

    @When("employee presses the Deny button")
    public void employeePressesTheDenyButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        // Create payload for loan denial
        Map<String, Object> denialData = new HashMap<>();
        denialData.put("approved", false);
        denialData.put("reason", "odbijen");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(denialData, headers);

        try {
            System.out.println("Sending loan denial request for loan ID: " + loanIdToDeny);
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/loans/admin/" + loanIdToDeny + "/approve",
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class
            );
            System.out.println("Loan denial response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error denying loan: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @When("employee presses the Approve button")
    public void employeePressesTheApproveButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        // Create payload for loan approval
        Map<String, Object> approvalData = new HashMap<>();
        approvalData.put("approved", true);
        approvalData.put("reason", "odobren");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(approvalData, headers);

        try {
            System.out.println("Sending loan approval request for loan ID: " + loanIdToApprove);
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/loans/admin/" + loanIdToApprove + "/approve",
                    HttpMethod.PUT,
                    requestEntity,
                    Map.class
            );
            System.out.println("Loan approval response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error approving loan: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @Then("the loan should be succesfully denied")
    public void theLoanShouldBeSuccesfullyDenied() {
        assertNull(exception, "No exception should be thrown during loan denial");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the success flag in the response
        Map<String, Object> responseBody = responseEntity.getBody();
        assertTrue((Boolean) responseBody.get("success"), "Response should indicate success");

        // Verify loan status is DENIED
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> nestedData = (Map<String, Object>) data.get("data");
        assertEquals("DENIED", nestedData.get("paymentStatus"), "Loan payment status should be DENIED");

        System.out.println("Successfully verified loan denial: " + nestedData.get("id"));
    }

    @Then("the loan should be succesfully approved")
    public void theLoanShouldBeSuccesfullyApproved() {
        assertNull(exception, "No exception should be thrown during loan approval");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the success flag in the response
        Map<String, Object> responseBody = responseEntity.getBody();
        assertTrue((Boolean) responseBody.get("success"), "Response should indicate success");

        // Verify loan status is APPROVED
        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> nestedData = (Map<String, Object>) data.get("data");
        assertEquals("APPROVED", nestedData.get("paymentStatus"), "Loan payment status should be APPROVED");

        System.out.println("Successfully verified loan approval: " + nestedData.get("id"));
    }
}