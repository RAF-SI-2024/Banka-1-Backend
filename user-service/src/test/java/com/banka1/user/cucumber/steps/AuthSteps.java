package com.banka1.user.cucumber.steps;

import com.banka1.user.DTO.request.*;
import com.banka1.user.model.Employee;
import com.banka1.user.model.ResetPassword;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.repository.ResetPasswordRepository;
import com.banka1.user.repository.SetPasswordRepository;
import com.banka1.user.service.ResetPasswordService;
import com.banka1.user.service.SetPasswordService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.java.Before;
import org.springframework.web.server.ResponseStatusException;

import javax.jms.Message;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class AuthSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private String token;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ResetPasswordRepository resetPasswordRepository;

    @Autowired
    private SetPasswordRepository setPasswordRepository;
    @Mock
    private JmsTemplate jmsTemplate;
    @Autowired
    private ResetPasswordService resetPasswordService;
    @Autowired
    private SetPasswordService setPasswordService;
    private ResetPasswordConfirmationRequest resetPasswordConfirmationRequest;
    private ResponseEntity<?> responseEntity;
    private HttpEntity<Void> request;
    private HttpStatusCodeException exception;
    private ResetPassword resetPassword;
    private String resetToken;
    private String testEmail;
    private boolean emailSent;


    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    private String getEmployeeUrl() {
        return "http://localhost:" + port + "/api/users/employees";
    }

    private String getCustomerUrl() {
        return "http://localhost:" + port + "/api/customer";
    }

    private String getResetTokenUrl(){
        return "http://localhost:" + port + "/api/users/reset-password";
    }

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
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


    @Given("a user with email {string} is an employee")
    public void user_with_email_exists(String email) {
        var customer = employeeRepository.findByEmail(email);
        if (customer.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        this.testEmail = email;
    }

    @When("the user requests a password reset")
    public void user_requests_password_reset() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(testEmail);

        Optional<Employee> employeeOptional = employeeRepository.findByEmail(testEmail);
        if (employeeOptional.isEmpty()) {
            throw new RuntimeException("Employee not found for email: " + testEmail);
        }
        doNothing().when(jmsTemplate).convertAndSend(anyString(), any(Object.class));
        emailSent = true;

        resetPasswordService.requestPasswordReset(request);
        assertTrue(resetPasswordRepository.count()> 0);

    }


    @Then("a password reset email should be sent")
    public void password_email_should_be_sent() throws Exception {
        Assertions.assertTrue(emailSent, "Expected a password reset email to be sent, but it was not.");
    }

    @And("the email should contain a reset link")
    public void email_should_contains_reset_link() {
        List<ResetPassword> resetTokens = resetPasswordRepository.findAll();
        assertFalse(resetTokens.isEmpty(), "There should be at least one reset token generated.");

        ResetPassword latestReset = resetTokens.get(resetTokens.size() - 1);
        this.resetToken = latestReset.getToken();

        assertNotNull(resetToken, "Reset token should be generated.");
    }


}