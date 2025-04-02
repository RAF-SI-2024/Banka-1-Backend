package com.banka1.notification.sender;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.service.EmailService;
import org.springframework.stereotype.Component;

@Component("email")
public class EmailSender implements NotificationSender {

    private final EmailService emailService;

    public EmailSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendToCustomer(NotificationDTO notification) {
        emailService.sendEmail(notification);
    }
}
