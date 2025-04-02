package com.banka1.banking.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NotificationDTO {
    private String email;
    private String subject;
    private String message;
    private String firstName;
    private String lastName;
    private String type;
    private Map<String, String> additionalData;
}
