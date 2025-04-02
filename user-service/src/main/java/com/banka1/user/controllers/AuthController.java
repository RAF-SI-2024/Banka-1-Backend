package com.banka1.user.controllers;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.service.BlackListTokenService;
import com.banka1.user.service.AuthService;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API" , description = "API za login, logout i osvežavanje JWT tokena")
public class AuthController {
    private final AuthService authService;
    private final BlackListTokenService blackListTokenService;

    @Operation(
        summary = "Login korisnika",
        description = "Loguje korisnika na sistem i vraća JWT token koji sadrži ID korisnika, rolu i set permisija."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Uspešan login", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "token": "jwt_token"
                    }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Neispravni podaci ili korisnik ne postoji", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik ne postoji."
                }
            """))
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Podaci za login",
        required = true,
        content = @Content(schema = @Schema(implementation = LoginRequest.class),
        examples = @ExampleObject(value = "{ \"email\": \"korisnik@primer.com\", \"password\": \"lozinkA123\" }")
    )) LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("token", token), null);
        } catch(Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @Operation(
        summary = "Logout korisnika",
        description = "Odjavljuje korisnika i blacklistuje trenutni JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešan logout", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Korisnik odjavljen"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "401", description = "Token nije prosleđen ili je neispravan/istekao.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Token nije prosleđen ili je neispravan/istekao."
                }
            """))
        )
    })
    @PostMapping("/logout")
    @Authorization
    public ResponseEntity<?> logout(@Parameter(
        description = "JWT token korisnika u Authorization header-u",
        required = true,
        example = "Bearer jwt_token"
    ) @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            blackListTokenService.blacklistToken(authService.getToken(authorization));
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", ResponseMessage.LOGOUT_SUCCESS.toString()), null);
        } catch(Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @Operation(
        summary = "Osvežavanje JWT tokena",
        description = "Generiše novi JWT token i blacklistuje stari token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešno generisan novi token", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "token": "new_jwt_token"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "401", description = "Token nije prosleđen ili je neispravan/istekao.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Token nije prosleđen ili je neispravan/istekao."
                }
            """))
        )
    })
    @PostMapping("/refresh-token")
    @Authorization
    public ResponseEntity<?> refreshToken(@Parameter(
        description = "JWT token korisnika u Authorization header-u",
        required = true,
        example = "Bearer jwt_token"
    ) @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            String oldToken = authService.getToken(authorization);
            String newToken = authService.recreateToken(oldToken);
            blackListTokenService.blacklistToken(oldToken);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("token", newToken), null);
        } catch(Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
