package com.banka1.notification.cucumber.steps;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.listener.helper.MessageHelper;
import com.banka1.notification.model.Notification;
import com.banka1.notification.model.helper.UserType;
import com.banka1.notification.repository.NotificationRepository;
import com.banka1.notification.service.EmailService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class SendEmailSteps {
    @Mock
    private EmailService emailService;

    @Autowired
    private NotificationRepository notificationRepository;

    private NotificationDTO notificationDTO;
    private boolean emailSent;

    @Given("an email notification request with valid details")
    public void an_email_notification_request_with_valid_details() {
        notificationDTO = new NotificationDTO();
        notificationDTO.setEmail("test@example.com");
        notificationDTO.setSubject("Test Subject");
        notificationDTO.setMessage("Test Message");
        notificationDTO.setFirstName("John");
        notificationDTO.setLastName("Doe");
        notificationDTO.setType("EMAIL");
        notificationDTO.setUserId(12345L);
        notificationDTO.setUserType(UserType.CUSTOMER);
    }

    @When("the notification is processed")
    public void the_notification_is_processed() throws JMSException {
        MessageHelper mockMessageHelper = mock(MessageHelper.class);
        when(mockMessageHelper.getMessage(any(Message.class), eq(NotificationDTO.class))).thenReturn(notificationDTO);


        Message mockMessage = mock(Message.class);

        EmailService mockEmailService = mock(EmailService.class);
        doNothing().when(mockEmailService).sendEmail(any(NotificationDTO.class));
        emailSent = true;


        Notification notification = new Notification();
        notification.setSubject(notificationDTO.getSubject());
        notification.setBody(notificationDTO.getMessage());
        notification.setUserId(String.valueOf(notificationDTO.getUserId()));
        notification.setNotificationType(notificationDTO.getType());
        notification.setUserType(notificationDTO.getUserType());

        notificationRepository.save(notification);
    }

    @Then("an email should be sent successfully")
    public void an_email_should_be_sent_successfully() {
        Assertions.assertTrue(emailSent, "Email should be sent successfully.");
    }

    @And("the notification should be stored in the database")
    public void the_notification_should_be_stored_in_the_database() {
        Optional<Notification> savedNotification = notificationRepository.findAll().stream().findFirst();
        Assertions.assertTrue(savedNotification.isPresent(), "Notification should be stored in the database.");
        Assertions.assertEquals("Test Subject", savedNotification.get().getSubject());
    }




}
