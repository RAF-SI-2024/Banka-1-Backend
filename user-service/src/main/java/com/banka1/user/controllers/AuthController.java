package com.banka1.user.controllers;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.security.JwtUtil;
import com.banka1.user.service.AuthService;
import com.banka1.user.service.BlackListTokenService;
import io.jsonwebtoken.Claims;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autentifikacija" , description = "Rute za login,logout i osvežavanje JWT tokena")
public class AuthController {

    private final AuthService authService;
    private final BlackListTokenService blackListTokenService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Login korisnika",
            description = "Loguje korisnika na sistem i vraća JWT token koji sadrži ID korisnika,rolu i set permisija."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",description = "Uspešan login",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"data\": { \"token\": \"jwt_token\" } }"))),
            @ApiResponse(responseCode = "400", description = "Neispravni podaci ili korisnik ne postoji",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Korisnik ne postoji.\" }")))
    })
    @RequestMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci za login",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = "{ \"email\": \"korisnik@primer.com\", \"password\": \"lozinkA123\" }"))
            ) LoginRequest loginRequest) {

        try {
            String token = authService.login(loginRequest);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("token",token)
            ));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }

    }

    @Operation(
            summary = "Logout korisnika",
            description = "Odjavljuje korisnika i blacklistuje trenutni JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200" , description = "Uspešan logout",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true , \"data\": { \"message\": \"Korisnik odjavljen\" } }"))),
            @ApiResponse(responseCode = "400" , description = "Token nije prosleđen ili je neispravan",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false , \"error\": \"Token nije pronađen ili je neispravan.\" }")))
    })
    @RequestMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(
                    description = "JWT token korisnika u Authorization header-u",
                    required = true,
                    example = "Bearer jwt_token")
            @RequestHeader(value = "Authorization",required = false) String authHeader){

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Token nije prosleđen ili je neispravan."
            ));
        }

        String token = authHeader.substring(7);
        blackListTokenService.blacklistToken(token);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("message","Korisnik odjavljen")
        ));

    }

    @Operation(
            summary = "Osvežavanje JWT tokena",
            description = "Generiše novi JWT token i blacklistuje stari token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uspešno generisan novi token",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"data\": { \"token\": \"new_jwt_token\" } }"))),
            @ApiResponse(responseCode = "400", description = "Token nije prosleđen ili je neispravan.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Token nije prosleđen ili je neispravan.\" }"))),
            @ApiResponse(responseCode = "403", description = "Token je istekao,blacklistovan ili nevažeći",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false , \"error\": \"Token je već istekao ili je nevažeći.\" }")))
    })
    @GetMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @Parameter(
                    description = "JWT token korisnika u Authorization header-u",
                    required = true,
                    example = "Bearer jwt_token")
            @RequestHeader(value = "Authorization",required = false) String authHeader){

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Token nije prosleđen ili je neispravan."
            ));
        }

        String oldToken = authHeader.substring(7);

        if(blackListTokenService.isTokenBlacklisted(oldToken)){
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Token je već istekao ili je nevažeći."
            ));
        }

        if(!jwtUtil.validateToken(oldToken)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Token nije validan"
            ));
        }

        Claims claims = jwtUtil.getClaimsFromToken(oldToken);
        Long userId = claims.get("userId",Long.class);
        String role = claims.get("role",String.class);
        List<String> permissions = claims.get("permissions",List.class);

        String newToken = jwtUtil.generateToken(userId,role,permissions);

        blackListTokenService.blacklistToken(oldToken);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("token",newToken)
        ));

    }

    @GetMapping("/protected")
    public ResponseEntity<?> protectedRoute(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Token je već istekao ili je nevažeći."));
        }
        return ResponseEntity.ok(Map.of("data", "Ovo je zaštićen sadržaj"));
    }


}
