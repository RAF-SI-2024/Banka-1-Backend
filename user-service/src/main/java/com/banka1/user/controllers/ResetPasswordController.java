package com.banka1.user.controllers;

import com.banka1.user.DTO.request.ResetPasswordConfirmationRequest;
import com.banka1.user.DTO.request.ResetPasswordRequest;
import com.banka1.user.service.ResetPasswordService;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/reset-password")
@Tag(name = "Reset Password API", description = "API za resetovanje lozinke")
public class ResetPasswordController {

    @Autowired
    private ResetPasswordService resetPasswordService;

    @PutMapping("/")
    @Operation(summary = "Podnosenje zahteva za resetovanje lozinke")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zahtev za resetovanje lozinke je uspešno poslat"),
            @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom slanja zahteva za resetovanje lozinke")
    })
    public ResponseEntity<?> requestPasswordReset(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            resetPasswordService.requestPasswordReset(resetPasswordRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", ResponseMessage.PASSWORD_RESET_REQUEST_SUCCESS.toString()), null);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @PostMapping("/")
    @Operation(summary = "Resetovanje lozinke (verifikacija mejla i postavljanje nove lozinke)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lozinka je uspešno resetovana"),
            @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom resetovanja lozinke")
    })
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordConfirmationRequest resetPasswordConfirmationRequest) {
        try {
            resetPasswordService.resetPassword(resetPasswordConfirmationRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", ResponseMessage.PASSWORD_RESET_SUCCESS.toString()), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }
}
