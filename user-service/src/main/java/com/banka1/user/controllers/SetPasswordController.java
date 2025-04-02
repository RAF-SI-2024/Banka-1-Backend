package com.banka1.user.controllers;


import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.service.SetPasswordService;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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
        @ApiResponse(responseCode = "200", description = "Lozinka uspešno postavljena.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "message": "Lozinka uspešno postavljena."
                }
            """))),
        @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom postavljanja lozinke", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Došlo je do greške prilikom postavljanja lozinke."
                }
            """)))
    })
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest dto) {
        try {
            setPasswordService.setPassword(dto);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", ResponseMessage.PASSWORD_SET_SUCCESS.toString()), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
