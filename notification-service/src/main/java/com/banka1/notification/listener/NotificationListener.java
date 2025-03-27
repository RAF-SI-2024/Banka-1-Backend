package com.banka1.notification.listener;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.listener.helper.MessageHelper;
import com.banka1.notification.sender.NotificationSender;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final NotificationSender emailSender;
    private final NotificationSender firebaseSender;

    public NotificationListener(MessageHelper messageHelper, @Qualifier("email") NotificationSender emailSender, @Qualifier("firebase") NotificationSender firebaseSender) {
        this.messageHelper = messageHelper;
        this.emailSender = emailSender;
        this.firebaseSender = firebaseSender;
    }

    private final MessageHelper messageHelper;

    @JmsListener(destination = "${destination.email}", concurrency = "5-10")
    public void onActivationMessage(Message message) throws JMSException {
        System.out.println("Message received: " + message);
        NotificationDTO notification = messageHelper.getMessage(message, NotificationDTO.class);

        if (notification.getType().equals("email")) {
            emailSender.sendToCustomer(notification);
        }
        if (notification.getType().equals("firebase")) {
            System.out.println(notification.getAdditionalData());
            firebaseSender.sendToCustomer(notification);
        }
    }
}
