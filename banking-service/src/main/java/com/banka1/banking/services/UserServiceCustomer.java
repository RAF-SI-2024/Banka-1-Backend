package com.banka1.banking.services;
import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.listener.MessageHelper;
import jakarta.jms.JMSException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserServiceCustomer {
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;

    @Value("${destination.customer}")
    private String destination;
    @Value("${destination.customer.email}")
    private String destinationEmail;

    public CustomerDTO getCustomerById(Long customerId) {
        var message = jmsTemplate.sendAndReceive(destination, session -> session.createTextMessage(messageHelper.createTextMessage(customerId)));
        CustomerDTO response;
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
        CustomerDTO response;
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
