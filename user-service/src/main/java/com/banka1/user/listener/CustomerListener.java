package com.banka1.user.listener;

import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.service.CustomerService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerListener {
    private final CustomerService customerService;
    private final MessageHelper messageHelper;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = "${destination.customer}", concurrency = "5-10")
    public void onActivationMessage(Message message) throws JMSException {
        var id = messageHelper.getMessage(message, Long.class);
        CustomerResponse customer = null;
        try {
            if (id != null)
                customer = customerService.findById(id);
        } catch (Exception e) {
            log.error("CustomerListener: ", e);
        }
        jmsTemplate.convertAndSend(message.getJMSReplyTo(), messageHelper.createTextMessage(customer));
    }

    @JmsListener(destination = "${destination.customer.email}", concurrency = "5-10")
    public void onGetCustomerByEmailMessage(Message message) throws JMSException {
        var email = messageHelper.getMessage(message, String.class);
        CustomerResponse customer = null;
        try {
            if (email != null)
                customer = customerService.findByEmail(email);
        } catch (Exception e) {
            log.error("CustomerListener (by email): ", e);
        }
        jmsTemplate.convertAndSend(message.getJMSReplyTo(), messageHelper.createTextMessage(customer));
    }

}
