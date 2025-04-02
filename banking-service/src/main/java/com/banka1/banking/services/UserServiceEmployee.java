package com.banka1.banking.services;

import com.banka1.banking.dto.EmployeeDTO;
import com.banka1.banking.listener.MessageHelper;
import jakarta.jms.JMSException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceEmployee {
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;

    @Value("${destination.employee.legal}")
    private String destination;

    public EmployeeDTO getEmployeeInLegal() {
        var message = jmsTemplate.sendAndReceive(destination, session -> session.createTextMessage(messageHelper.createTextMessage("")));
        EmployeeDTO response;
        try {
            response = messageHelper.getMessage(message, EmployeeDTO.class);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        if (response == null) {
            throw new IllegalArgumentException("Korisnik nije pronađen ili API nije vratio očekivani format.");
        }

        return response;
    }
}
