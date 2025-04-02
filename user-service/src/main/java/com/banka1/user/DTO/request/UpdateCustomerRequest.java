package com.banka1.user.DTO.request;

import com.banka1.user.model.helper.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Nove informacije o musteriji. Null vrednost polja oznacava da se to polje ne menja.")
public class UpdateCustomerRequest {
    private String firstName;

    private String lastName;

    private String username;

    private String birthDate;

    private Gender gender;

    private String email;

    private String phoneNumber;

    private String address;
}
