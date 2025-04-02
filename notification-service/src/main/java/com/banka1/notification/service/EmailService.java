package com.banka1.notification.service;

import com.banka1.notification.DTO.response.NotificationDTO;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Service
@Transactional
public class EmailService {

    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private String smtpPort;

    @Value("${mail.smtp.auth}")
    private String smtpAuth;

    @Value("${mail.smtp.starttls.enable}")
    private String startTls;

    @Value("${mail.from}")
    private String fromEmail;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    public void sendEmail(NotificationDTO emailDto) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", smtpAuth);
        properties.put("mail.smtp.starttls.enable", startTls);

        System.out.println("Sending email to " + emailDto.getEmail());

        Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDto.getEmail()));
            message.setSubject(MimeUtility.encodeText(emailDto.getSubject(), "UTF-8", "B"));
            message.setContent(emailDto.getMessage(), "text/plain; charset=UTF-8");

            Transport.send(message);

            System.out.println("Email sent successfully to " + emailDto.getEmail());
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
