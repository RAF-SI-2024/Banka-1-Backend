package com.banka1.notification.service;

import com.banka1.notification.repository.CustomerDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebaseService {

    private final CustomerDeviceRepository customerDeviceRepository;

    public FirebaseService(CustomerDeviceRepository customerDeviceRepository) {
        this.customerDeviceRepository = customerDeviceRepository;
    }

    public void sendNotificationToCustomer(String title, String body, Long customerId, Map<String, String> data) {
        customerDeviceRepository.findByCustomerId(customerId).forEach(customerDevice -> sendNotification(title, body, customerDevice.getDeviceToken(), data));
    }

    public void broadcastNotification(String title, String body, Map<String, String> data) {
        // send message to topic test

        if (data == null) {
            data = Map.of();
        }

        Message message = Message.builder()
                .setTopic("test")
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Notifikacija poslata: " + response);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendNotification(String title, String body, String deviceToken, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Notifikacija poslata: " + response);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }
}
