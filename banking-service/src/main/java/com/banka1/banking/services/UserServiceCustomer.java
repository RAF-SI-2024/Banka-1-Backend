package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.listener.MessageHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceCustomer {
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final ObjectMapper objectMapper;

    @Value("${destination.customer}")
    private String destination;
    @Value("${destination.customer.email}")
    private String destinationEmail;

    public CustomerDTO getCustomerById(Long customerId) {
        var message = jmsTemplate.sendAndReceive(destination, session -> session.createTextMessage(messageHelper.createTextMessage(customerId)));
        CustomerDTO response = null;
        try {
            response = messageHelper.getMessage(message, CustomerDTO.class);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        if (response == null) {
            throw new IllegalArgumentException("Korisnik nije pronađen ili API nije vratio očekivani format.");
        }

        return response;
    }

    public CustomerDTO getCustomerByEmail(String email){
        var message = jmsTemplate.sendAndReceive(destinationEmail, session -> session.createTextMessage(messageHelper.createTextMessage(destinationEmail)));
        CustomerDTO response = null;
        try {
            response = messageHelper.getMessage(message, CustomerDTO.class);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        if (response == null) {
            throw new IllegalArgumentException("Korisnik nije pronađen ili API nije vratio očekivani format.");
        }

        return response;
    }

}
