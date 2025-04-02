package com.banka1.banking.controllers;

import com.banka1.banking.aspect.AccountAuthorization;
import com.banka1.banking.dto.OtpTokenDTO;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.repository.TransferRepository;
import com.banka1.banking.services.OtpTokenService;
import com.banka1.banking.services.TransferService;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP verifikacija", description = "Ruta za validaciju kreiranog transfera")
@Slf4j
public class OtpTokenController {

    private final OtpTokenService otpTokenService;
    private final TransferRepository transferRepository;
    private final TransferService transferService;

    @Operation(
            summary = "Verifikacija OTP koda",
            description = "Proverava da li je uneti OTP kod validan i još uvek važeći."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP validan, transakcija izvršena.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"data\": { \"message\": \"OTP validan, transakcija izvršena.\" } }"))
            ),
            @ApiResponse(responseCode = "401", description = "Nevalidan OTP kod ili je već iskorišćen.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Nevalidan OTP kod ili je već iskorišćen.\" }"))
            ),
            @ApiResponse(responseCode = "408", description = "OTP kod je istekao.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"OTP kod je istekao.\" }"))
            )
    })
    @PostMapping("/verification")
    @AccountAuthorization(customerOnlyOperation = true)
    public ResponseEntity<?> verifyOtp(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Podaci za verifikaciju OTP koda",
            required = true,
            content = @Content(schema = @Schema(implementation = OtpTokenDTO.class),
                    examples = @ExampleObject(value = "{ \"transakcijaId\": 1, \"otpKod\": \"123456\" }")
            )) OtpTokenDTO otpTokenDTO) {

        try {
            Long transferId = otpTokenDTO.getTransferId();
            String otpCode = otpTokenDTO.getOtpCode();

            if (otpTokenService.isOtpExpired(transferId)) {
                log.info("ERROR: OTP kod je istekao.");
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT),
                        false, null, "OTP kod je istekao.");
            }

            if (!otpTokenService.isOtpValid(transferId, otpCode)) {
                log.info("ERROR: Nevalidan OTP kod ili je već iskorišćen.");
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.UNAUTHORIZED),
                        false, null, "Nevalidan OTP kod ili je već iskorišćen.");
            }


            Optional<Transfer> optionalTransfer = transferRepository.findById(transferId);

            if(optionalTransfer.isPresent()){
                Transfer transfer = optionalTransfer.get();
                if(transfer.getStatus().equals(TransferStatus.CANCELLED)){
                    return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST),
                            false,null,"Transfer je otkazan.");
                }
                otpTokenService.markOtpAsUsed(transferId, otpCode);


                try {
                    transferService.processTransfer(transferId);
                } catch (Exception e){
                    return ResponseTemplate.create(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR),
                            false, null, "Transakcija nije uspela: " + e.getMessage());
                }


            }


            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK),
                    true, Map.of("message", "OTP validan, transakcija izvršena."), null);

        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}

