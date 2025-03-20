package com.banka1.banking.steps;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.services.UserServiceCustomer;
import io.cucumber.java.en.*;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

public class UserServiceCustomerStepDefinitions {

    @Autowired
    private UserServiceCustomer userServiceCustomer;

    @MockBean
    private JmsTemplate jmsTemplate;

    @MockBean
    private MessageHelper messageHelper;

    private CustomerDTO customerDTO;
    private Exception exception;

    @Given("customer with id {long} exists")
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

