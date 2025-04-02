package com.banka1.notification;

import com.banka1.notification.DTO.response.NotificationDTO;
import com.banka1.notification.listener.NotificationListener;
import com.banka1.notification.listener.helper.MessageHelper;
import com.banka1.notification.model.Notification;
import com.banka1.notification.model.helper.UserType;
import com.banka1.notification.repository.NotificationRepository;
import com.banka1.notification.sender.EmailSender;
import com.banka1.notification.sender.FirebaseSender;
import com.banka1.notification.sender.NotificationSender;
import com.banka1.notification.service.EmailService;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Transactional
public class NotificationApplicationTests {

	@Mock
	private MessageHelper messageHelper;
	@Mock
	private EmailSender emailSender;

	@Mock
	private FirebaseSender firebaseSender;

	@Autowired
	private NotificationRepository notificationRepository;
	@Mock
	private Message message;
	@InjectMocks
	private NotificationListener notificationListener;

	private NotificationDTO notificationDTO;

	@BeforeEach
	void setUp() throws JMSException {

		notificationListener = new NotificationListener(messageHelper, emailSender, firebaseSender);
		notificationDTO = new NotificationDTO();
		notificationDTO.setEmail("test@example.com");
		notificationDTO.setSubject("Test Subject");
		notificationDTO.setMessage("Test Message");
		notificationDTO.setFirstName("John");
		notificationDTO.setLastName("Doe");
		notificationDTO.setType("email");
		notificationDTO.setUserId(12345L);
		notificationDTO.setUserType(UserType.CUSTOMER);

		// Then: Verify email service was called
		when(messageHelper.getMessage(any(), eq(NotificationDTO.class))).thenReturn(notificationDTO);

		notificationListener.onActivationMessage(message);

		verify(emailSender, times(1)).sendToCustomer(any(NotificationDTO.class));

	}

	@Test
	void testCompleteNotificationFlow() throws JMSException {
		// Given: A message received in JMS queue
		when(messageHelper.getMessage(any(), eq(NotificationDTO.class))).thenReturn(notificationDTO);
		// When: The listener processes the message
		notificationListener.onActivationMessage(message);

		Notification notification = new Notification();
		notification.setSubject(notificationDTO.getSubject());
		notification.setBody(notificationDTO.getMessage());
		notification.setUserId(String.valueOf(notificationDTO.getUserId()));
		notification.setNotificationType(notificationDTO.getType());
		notification.setUserType(notificationDTO.getUserType());

		notificationRepository.save(notification);

		// Then: Verify that the email service was called
		verify(emailSender, times(2)).sendToCustomer(any(NotificationDTO.class));

		// Then: Check if the notification is saved in the database
		Optional<Notification> savedNotification = notificationRepository.findAll()
				.stream()
				.findFirst();

		assertThat(savedNotification).isPresent();
		assertThat(savedNotification.get().getSubject()).isEqualTo("Test Subject");
	}
}