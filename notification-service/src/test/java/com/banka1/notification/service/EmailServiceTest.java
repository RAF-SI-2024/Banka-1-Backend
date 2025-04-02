package com.banka1.notification.service;

import com.banka1.notification.DTO.response.NotificationDTO;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @InjectMocks
    private EmailService emailService; // Inject mocks into the service

    @BeforeEach
    void setUp() {
        // Set private fields in EmailService using ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "smtpHost", "smtp.test.com");
        ReflectionTestUtils.setField(emailService, "smtpPort", "587");
        ReflectionTestUtils.setField(emailService, "smtpAuth", "true");
        ReflectionTestUtils.setField(emailService, "startTls", "true");
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "username", "testUser");
        ReflectionTestUtils.setField(emailService, "password", "testPass");
    }

    @Test
    void testSendEmail() throws Exception {
        NotificationDTO emailDto = new NotificationDTO();
        emailDto.setEmail("receiver@example.com");
        emailDto.setSubject("Test Subject");
        emailDto.setMessage("Test Message");

        // Mock static Transport.send()
        try (var transportMock = Mockito.mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(Message.class))).thenAnswer(invocation -> null);
            emailService.sendEmail(emailDto);
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }

    }
}
