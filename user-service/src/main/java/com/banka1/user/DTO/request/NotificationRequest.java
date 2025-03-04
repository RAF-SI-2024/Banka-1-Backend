package com.banka1.user.DTO.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {
    private String email;
    private String subject;
    private String message;
    private String firstName;
    private String lastName;
    private String type;
}
