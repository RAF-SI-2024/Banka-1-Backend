package com.banka1.notification.listener;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.listener.helper.MessageHelper;
import com.banka1.notification.sender.EmailSender;
import com.banka1.notification.service.EmailService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private EmailSender emailSender;

    @Mock
    private Message message;

    @InjectMocks
    private NotificationListener notificationListener;

    private NotificationDTO mockNotification;

    @BeforeEach
    void setUp() throws JMSException {
        mockNotification = new NotificationDTO();
        mockNotification.setEmail("test@example.com");
        mockNotification.setSubject("Test Subject");
        mockNotification.setMessage("Test Message");
        mockNotification.setType("email");

        when(messageHelper.getMessage(any(Message.class), eq(NotificationDTO.class)))
                .thenReturn(mockNotification);
    }

    @Test
    void testOnActivationMessage() throws JMSException {
        notificationListener.onActivationMessage(message);

        verify(messageHelper, times(1)).getMessage(eq(message), eq(NotificationDTO.class));
        verify(emailSender, times(1)).sendToCustomer(eq(mockNotification));
    }
}
