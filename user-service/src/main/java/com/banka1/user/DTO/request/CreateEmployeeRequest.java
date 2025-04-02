package com.banka1.user.DTO.request;

import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import com.banka1.common.model.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "Ime ne može biti prazno")
    @Size(min = 2, max = 50, message = "Ime mora imati između 2 i 50 karaktera")
    private String firstName;

    @NotBlank(message = "Prezime ne može biti prazno")
    @Size(min = 2, max = 50, message = "Prezime mora imati između 2 i 50 karaktera")
    private String lastName;

    @NotNull(message = "Datum rođenja je obavezan")
    private LocalDate birthDate;

    @NotNull(message = "Pol je obavezan")
    private Gender gender;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email mora biti validan")
    private String email;

    @NotBlank(message = "Broj telefona je obavezan")
    private String phoneNumber;

    @NotBlank(message = "Adresa je obavezna")
    private String address;

    @NotBlank(message = "Username je obavezan")
    @Size(min = 2, max = 50, message = "Username mora imati između 2 i 50 karaktera")
    private String username;

    @NotBlank(message = "Pozicija je obavezna")
    private Position position;

    @NotBlank(message = "Departman je obavezan")
    private Department department;

    private Boolean active = true; // Default vrednost

    private Boolean isAdmin = false; // default vrednost

    @NotNull(message = "Lista permisija je obavezna (moze biti prazna)")
    private List<Permission> permissions;
}
