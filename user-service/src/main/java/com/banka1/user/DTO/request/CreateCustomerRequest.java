package com.banka1.user.DTO.request;

import com.banka1.user.DTO.banking.CreateAccountWithoutOwnerIdDTO;
import com.banka1.user.model.helper.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {
    @NotBlank(message = "Ime ne može biti prazno")
    @Size(min = 2, max = 50, message = "Ime mora imati između 2 i 50 karaktera")
    private String firstName;

    @NotBlank(message = "Prezime ne može biti prazno")
    @Size(min = 2, max = 50, message = "Prezime mora imati između 2 i 50 karaktera")
    private String lastName;

    @NotBlank(message = "Username je obavezan")
    @Size(min = 2, max = 50, message = "Username mora imati između 2 i 50 karaktera")
    private String username;

    @NotNull(message = "Datum rođenja je obavezan")
    private String birthDate;

    @NotNull(message = "Pol je obavezan")
    private Gender gender;

    @NotBlank(message = "Email je obavezan")
    @Size(min = 2, max = 50, message = "Email mora imati između 2 i 50 karaktera")
    private String email;

    @NotBlank(message = "Broj telefona je obavezan")
    private String phoneNumber;

    @NotBlank(message = "Adresa je obavezan")
    private String address;

    @NotNull(message = "Informacije o racunu su obavezne")
    @Schema(description = "Informacije o racunu koji ce biti automatski kreiran")
    private CreateAccountWithoutOwnerIdDTO accountInfo;
}
