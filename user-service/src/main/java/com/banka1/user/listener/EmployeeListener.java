package com.banka1.user.listener;

import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.service.EmployeeService;
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
public class EmployeeListener {
    private final EmployeeService employeeService;
    private final MessageHelper messageHelper;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = "${destination.employee.legal}", concurrency = "5-10")
    public void onGetEmployeeInLegal(Message message) throws JMSException {
        EmployeeResponse employee = null;
        try {
            employee = employeeService.findInLegal();
        } catch (Exception e) {
            log.error("EmployeeListener: ", e);
        }
        jmsTemplate.convertAndSend(message.getJMSReplyTo(), messageHelper.createTextMessage(employee));
    }
}
