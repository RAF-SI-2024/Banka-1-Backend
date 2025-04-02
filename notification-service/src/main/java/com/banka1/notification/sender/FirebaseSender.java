package com.banka1.notification.sender;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.service.FirebaseService;
import org.springframework.stereotype.Component;

@Component("firebase")
public class FirebaseSender implements NotificationSender {

    private final FirebaseService firebaseService;

    public FirebaseSender(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @Override
    public void sendToCustomer(NotificationDTO notification) {
        firebaseService.sendNotificationToCustomer(notification.getSubject(), notification.getMessage(), notification.getUserId(), notification.getAdditionalData());
    }
}
