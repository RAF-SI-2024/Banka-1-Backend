package com.banka1.user.controllers;


import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.service.SetPasswordService;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/set-password")
@Tag(name = "Set Password API", description = "API za postavljanje prve lozinke")
public class SetPasswordController {
    private final SetPasswordService setPasswordService;

    public SetPasswordController(SetPasswordService setPasswordService) {this.setPasswordService = setPasswordService;}


    @PostMapping
    @Operation(summary = "Postavljanje lozinke/verfikacija mejla")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lozinka je uspešno resetovana"),
            @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom resetovanja lozinke")
    })
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest dto) {
        try {
            setPasswordService.setPassword(dto);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", ResponseMessage.PASSWORD_RESET_SUCCESS.toString()), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }
}
