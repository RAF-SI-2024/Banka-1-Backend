package com.banka1.banking.dto;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@RequiredArgsConstructor
@Data
public class CustomerDTO {
    @NonNull
    private Long id;

    @NonNull
    private String firstName;

    @NonNull 
    private String lastName;

    @NonNull
    private String birthDate;

    @NonNull
    private String email;

    @NonNull
    private String phoneNumber;

    @NonNull
    private String address;
  
    private List<String> permissions;

}
