package com.banka1.user.controllers;

import com.banka1.user.DTO.request.ResetPasswordConfirmationRequest;
import com.banka1.user.DTO.request.ResetPasswordRequest;
import com.banka1.user.service.ResetPasswordService;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users/reset-password")
@Tag(name = "Reset Password API", description = "API za resetovanje lozinke")
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @PutMapping("/")
    @Operation(summary = "Podnošenje zahteva za resetovanje lozinke")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Zahtev za resetovanje lozinke je uspešno poslat", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Zahtev za resetovanje lozinke je uspešno poslat."
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom slanja zahteva za resetovanje lozinke", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Email adresa nije pronadjena."
                }
            """))
        )
    })
    public ResponseEntity<?> requestPasswordReset(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            resetPasswordService.requestPasswordReset(resetPasswordRequest);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", ResponseMessage.PASSWORD_RESET_REQUEST_SUCCESS.toString()), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PostMapping("/")
    @Operation(summary = "Resetovanje lozinke (verifikacija mejla i postavljanje nove lozinke)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lozinka je uspešno resetovana", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Lozinka je uspešno resetovana"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom resetovanja lozinke", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Došlo je do greške prilikom resetovanja lozinke"
                }
            """))
        )
    })
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordConfirmationRequest resetPasswordConfirmationRequest) {
        try {
            resetPasswordService.resetPassword(resetPasswordConfirmationRequest);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", ResponseMessage.PASSWORD_RESET_SUCCESS.toString()), null);
        } catch (NoSuchElementException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, e.getMessage());
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
