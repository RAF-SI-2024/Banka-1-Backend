package com.banka1.user.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Zahtev za logovanje korisnika")
@Data
public class LoginRequest {
    @Schema(description = "Email korisnika", example = "korisnik@primer.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String email;
    @Schema(description = "Lozinka korisnika", example = "lozinkA123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String password;
}
