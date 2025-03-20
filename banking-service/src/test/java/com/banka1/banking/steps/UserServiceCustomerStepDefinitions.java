package com.banka1.banking.steps;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.services.UserServiceCustomer;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = UserServiceCustomer.class)
public class UserServiceCustomerStepDefinitions {

    @InjectMocks
    private UserServiceCustomer userServiceCustomer;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessageHelper messageHelper;

    private CustomerDTO customerDTO;
    private Exception exception;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // Ispravlja NullPointerException na mock-ovima
    }

    @Given("customer with id {long} exists in User Service")
    public void customerWithIdExists(Long customerId) throws JMSException {
        customerDTO = new CustomerDTO();
        customerDTO.setId(customerId);
        customerDTO.setFirstName("Marko");
        customerDTO.setLastName("Markovic");
        customerDTO.setEmail("marko@example.com");

        Message mockMessage = Mockito.mock(Message.class);

        Mockito.when(jmsTemplate.sendAndReceive(Mockito.anyString(), Mockito.any()))
                .thenReturn(mockMessage);

        Mockito.when(messageHelper.getMessage(mockMessage, CustomerDTO.class))
                .thenReturn(customerDTO);
    }

    @Given("customer with id {long} does not exist")
    public void customerWithIdDoesNotExist(Long customerId) throws JMSException {
        Message mockMessage = Mockito.mock(Message.class);

        Mockito.when(jmsTemplate.sendAndReceive(Mockito.anyString(), Mockito.any()))
                .thenReturn(mockMessage);

        Mockito.when(messageHelper.getMessage(mockMessage, CustomerDTO.class))
                .thenReturn(null);
    }

    @When("retrieving customer data by id {long}")
    public void retrievingCustomerDataById(Long customerId) {
        try {
            customerDTO = userServiceCustomer.getCustomerById(customerId);
        } catch (Exception e) {
            exception = e;
        }
    }

    @Then("customer data should be returned successfully")
    public void customerDataReturnedSuccessfully() {
        Assertions.assertNotNull(customerDTO);
        Assertions.assertEquals("Marko", customerDTO.getFirstName());
        Assertions.assertEquals("Markovic", customerDTO.getLastName());
        Assertions.assertEquals("marko@example.com", customerDTO.getEmail());
    }

    @Then("retrieving customer data should fail with message {string}")
    public void retrievingCustomerDataShouldFail(String expectedMessage) {
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(expectedMessage, exception.getMessage());
    }
}
