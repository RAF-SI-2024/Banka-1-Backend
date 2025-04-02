package com.banka1.banking.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizedPersonDTO {
    private String firstName;
    private String lastName;
    private String birthDate;
    private String phoneNumber;
    private Long companyID;
    private String email;
}
