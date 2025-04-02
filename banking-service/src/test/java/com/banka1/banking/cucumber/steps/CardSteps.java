package com.banka1.banking.cucumber.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

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

public class CardSteps {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String token;
    private ResponseEntity<Map> responseEntity;
    private HttpStatusCodeException exception;
    private Map<String, Object> cardData;

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
    @Given("customer is logged into the banking portal for cards")
    public void customerIsLoggedIntoBankingPortalForCards() {
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

    @And("customer navigates to card page")
    public void customerNavigatesToCardPage() {
        System.out.println("Customer navigates to card page (simulated)");
    }

    @When("customer fills out the card form")
    public void customerFillsOutTheCardForm() {
        cardData = new HashMap<>();
        cardData.put("accountID", 106);
        cardData.put("cardBrand", "VISA");
        cardData.put("cardType", "DEBIT");
        cardData.put("authorizedPerson", null);
        cardData.put("company", null);
        System.out.println("Card form completed with account ID: " + cardData.get("accountID") + " and card type: " + cardData.get("cardType"));
    }

    @And("customer presses the Continue button for cards")
    public void customerPressesTheContinueButton() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(cardData, headers);

        try {
            System.out.println("Sending card creation request with data: " + cardData);
            responseEntity = restTemplate.exchange(
                    getBaseUrl() + "/cards/",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            System.out.println("Card creation response: " + responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            exception = e;
            System.err.println("Error creating card: " + e.getStatusCode());
            System.err.println("Error response: " + e.getResponseBodyAsString());
        }
    }

    @Then("customer should see a success message for cards")
    public void customerShouldSeeASuccessMessage() {
        assertNull(exception, "No exception should be thrown");
        assertNotNull(responseEntity, "Response should not be null");
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode(), "Expected 201 CREATED status");

        Map<String, Object> responseBody = responseEntity.getBody();
        assertTrue((Boolean) responseBody.get("success"), "Response should indicate success");

        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        assertNotNull(data.get("id"), "Response should contain card ID");
        assertNotNull(data.get("message"), "Response should contain success message");

        System.out.println("Card successfully created with ID: " + data.get("id"));
    }
}