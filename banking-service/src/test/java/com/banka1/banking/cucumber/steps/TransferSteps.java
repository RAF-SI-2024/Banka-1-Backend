package com.banka1.banking.cucumber.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

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

public class TransferSteps {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private String token;
    private ResponseEntity<Map> responseEntity;
    private HttpStatusCodeException exception;
    private Map<String, Object> internalTransferData;
    private Map<String, Object> moneyTransferData;

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
    @Given("customer is logged into the banking portal for transfers")
    public void customerIsLoggedIntoBankingPortal() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", "marko.markovic@banka.com");
        loginData.put("password", "M@rko12345");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8081/api/auth/login", loginData, Map.class);

            token = (String) ((Map<String, Object>) response.getBody().get("data")).get("token");
            assertNotNull(token, "Token should be generated during login");
            System.out.println("Customer authenticated with token length: " + token.length());
        } catch (RestClientException e) {
            fail("Login failed: " + e.getMessage());
        }
    }

    @And("customer navigates to transfer page")
    public void customerNavigatesToTransferPage() {
        System.out.println("Customer navigates to transfer page (simulated)");
    }

    @And("customer navigates to payment page")
    public void customerNavigatesToPaymentPage() {
        System.out.println("Customer navigates to payment page (simulated)");
    }

    @When("customer fills out the transfer form")
    public void customerFillsOutTheTransferForm() {
        internalTransferData = new HashMap<>();
        internalTransferData.put("fromAccountId", 106);
        internalTransferData.put("toAccountId", 107);
        internalTransferData.put("amount", 1000);
    }

    @When("customer does not fill out the transfer form")
    public void customerDoesNotFillOutTheTransferForm() {
        internalTransferData = new HashMap<>();
        internalTransferData.put("fromAccountId", 106);
    }

    @When("customer fills out payment form")
    public void customerFillsOutPaymentForm() {
        moneyTransferData = new HashMap<>();
        moneyTransferData.put("fromAccountNumber", "111000100000000101");
        moneyTransferData.put("recipientAccount", "111000100011000101");
        moneyTransferData.put("amount", 1000);
        moneyTransferData.put("receiver", "Marko Marković");
        moneyTransferData.put("adress", "Knez Mihailova 7");
        moneyTransferData.put("payementCode", "289");
        moneyTransferData.put("payementReference", "222");
        moneyTransferData.put("payementDescription", "Test Payment");
    }

    @When("customer does not fill out the payment form")
    public void customerDoesNotFillOutThePaymentForm() {
        moneyTransferData = new HashMap<>();
        moneyTransferData.put("fromAccountNumber", "111000100000000101");
    }

    @And("customer presses the Continue button")
    public void customerPressesTheContinueButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(internalTransferData, headers);

        try {
            System.out.println("Sending internal transfer request with token");
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/internal-transfer",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            System.out.println("Internal transfer response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error with internal transfer: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @And("customer presses Continue button")
    public void customerPressesContinueButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(moneyTransferData, headers);

        try {
            System.out.println("Sending money transfer request with token");
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/money-transfer",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            System.out.println("Money transfer response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error with money transfer: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @Then("customer should be prompted to enter verification code")
    public void customerShouldBePromptedToEnterVerificationCode() {
        assertNull(exception, "No exception should be thrown");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "Expected 200 OK status");
    }

    @Then("customer should see an error message")
    public void customerShouldSeeAnErrorMessage() {
        assertNotNull(exception, "Exception should be thrown for invalid request");
        assertTrue(exception.getStatusCode().is4xxClientError(),
                "Should return a client error status code");

        String errorResponse = exception.getResponseBodyAsString();
        System.out.println("Received expected error: " + errorResponse);
    }
}