package com.banka1.banking.controllers;

import com.banka1.banking.dto.InternalTransferDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.models.Account;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "Rute za upravljanje i kreiranje transfera")
public class TransferController {

    private final TransferService transferService;

    @Operation(
            summary = "Interni prenos",
            description = "Kreira interni prenos novca između računa istog korisnika, ako su u istoj valuti."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interni prenos uspešno kreiran",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, { \"data\": \n \t \"message\": \"Interni prenos uspešno kreiran.\", \n \t \"transferId\": 12345 \n} \n}"))
            ),
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nedovoljno sredstava",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Nevalidni podaci ili nedovoljno sredstava.\" }"))
            )
    })
    @PostMapping("/internal-transfer")
    public ResponseEntity<?> internalTransfer(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci za interni transfer",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InternalTransferDTO.class),
                            examples = @ExampleObject(value = "{ \"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 500.0 }"))
            ) InternalTransferDTO transferDTO) {

        try {

            if (!transferService.validateInternalTransfer(transferDTO)){
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST),
                        false, null, "Nevalidni podaci ili nedovoljno sredstava.");
            }

            Long transferId = transferService.createInternalTransfer(transferDTO);

//            Long fromAccountId = transferDTO.getFromAccountId();
//            Long toAccountId = transferDTO.getToAccountId();
//            Double amount = transferDTO.getAmount();

            return ResponseTemplate.create(ResponseEntity.ok(),true, Map.of("message","Interni prenos uspešno kreiran.","transferId",transferId),null);


        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);

        }

    }

    @Operation(
            summary = "Prenos novca između korisnika",
            description = "Kreira prenos novca između različitih korisnika ako su računi iste valute."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Prenos novca uspešno kreiran",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, { \"data\": \n \t \"message\": \"Transfer novca uspešno kreiran.\", \n \t \"transferId\": 12345 \n} \n}"))
            ),
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nedovoljno sredstava",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Nevalidni podaci ili nedovoljno sredstava.\" }"))
            )
    })
    @PostMapping("/money-transfer")
    public ResponseEntity<?> moneyTransfer(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci za prenos novca",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MoneyTransferDTO.class),
                            examples = @ExampleObject(value = "{"
                                    + "  \"fromAccountId\": 1,"
                                    + "  \"toAccountId\": 3,"
                                    + "  \"amount\": 200.0,"
                                    + "  \"receiver\": \"Marko Marković\","
                                    + "  \"adress\": \"Kralja Petra 12\","
                                    + "  \"payementCode\": \"123\","
                                    + "  \"payementReference\": \"2024-0001\","
                                    + "  \"payementDescription\": \"Uplata za račun\""
                                    + "}"))
            ) MoneyTransferDTO transferDTO) {

        try {

            if (!transferService.validateMoneyTransfer(transferDTO)){
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST),
                        false, null, "Nevalidni podaci ili nedovoljno sredstava.");
            }

            Long transferId = transferService.createMoneyTransfer(transferDTO);

//            Long fromAccountId = transferDTO.getFromAccountId();
//            Long toAccountId = transferDTO.getToAccountId();
//            Double amount = transferDTO.getAmount();

            return ResponseTemplate.create(ResponseEntity.ok(),true, Map.of("message","Transfer novca uspešno kreiran.","transferId",transferId),null);

        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);

        }
    }


}
