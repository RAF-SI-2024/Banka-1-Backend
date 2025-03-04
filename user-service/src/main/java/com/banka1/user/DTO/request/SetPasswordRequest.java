package com.banka1.user.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetPasswordRequest {
    @NotBlank
    @Schema(description = "Token iz mejla")
    private String token;
    @NotBlank
    @Schema(description = "Password novog korisnika")
    private String password;
}
