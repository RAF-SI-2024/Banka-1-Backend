package com.banka1.notification;

import com.banka1.notification.service.FirebaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Test implements CommandLineRunner {

    private final FirebaseService firebaseService;

    public Test(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @Override
    public void run(String... args) {
        System.out.println("---------------------------");

        firebaseService.broadcastNotification("Hello from Banka1", "asdasdsad", null);
    }
}
