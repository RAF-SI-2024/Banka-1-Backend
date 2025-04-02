package com.banka1.notification.repository;

import com.banka1.notification.model.Notification;
import com.banka1.notification.model.helper.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testSaveAndFindById() {
        // Given
        Notification notification = new Notification();
        notification.setSubject("Welcome Email");
        notification.setBody("Welcome to our service!");
        notification.setUserId("1001");
        notification.setNotificationType("EMAIL");
        notification.setUserType(UserType.CUSTOMER);
        notification.setSentAt(LocalDateTime.now());

        // When
        Notification savedNotification = notificationRepository.save(notification);
        Optional<Notification> foundNotification = notificationRepository.findById(savedNotification.getId());

        // Then
        assertThat(foundNotification).isPresent();
        assertThat(foundNotification.get().getSubject()).isEqualTo("Welcome Email");
        assertThat(foundNotification.get().getBody()).isEqualTo("Welcome to our service!");
        assertThat(foundNotification.get().getUserId()).isEqualTo("1001");
        assertThat(foundNotification.get().getNotificationType()).isEqualTo("EMAIL");
        assertThat(foundNotification.get().getUserType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    void testFindAll() {
        // Given
        Notification customerNotification = new Notification();
        customerNotification.setSubject("Promo Offer");
        customerNotification.setBody("Special discount for you!");
        customerNotification.setUserId("2002");
        customerNotification.setNotificationType("SMS");
        customerNotification.setUserType(UserType.CUSTOMER);

        Notification employeeNotification = new Notification();
        employeeNotification.setSubject("Work Reminder");
        employeeNotification.setBody("Team meeting at 10 AM");
        employeeNotification.setUserId("3003");
        employeeNotification.setNotificationType("EMAIL");
        employeeNotification.setUserType(UserType.EMPLOYEE);

        notificationRepository.save(customerNotification);
        notificationRepository.save(employeeNotification);

        // When
        List<Notification> notifications = notificationRepository.findAll();

        // Then
        assertThat(notifications).hasSize(2);
    }

    @Test
    void testDelete() {
        // Given
        Notification notification = new Notification();
        notification.setSubject("Delete Test");
        notification.setBody("This will be deleted.");
        notification.setUserId("4004");
        notification.setNotificationType("PUSH");
        notification.setUserType(UserType.EMPLOYEE);

        Notification savedNotification = notificationRepository.save(notification);

        // When
        notificationRepository.deleteById(savedNotification.getId());
        Optional<Notification> deletedNotification = notificationRepository.findById(savedNotification.getId());

        // Then
        assertThat(deletedNotification).isEmpty();
    }
}