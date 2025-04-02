package com.banka1.banking.controllers;
import com.banka1.banking.aspect.ReceiverAuthorization;
import com.banka1.banking.dto.ReceiverDTO;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.services.ReceiverService;
import com.banka1.banking.utils.ResponseTemplate;
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
@RequestMapping("/receiver")
@RequiredArgsConstructor
@Tag(name = "Primaoci placanja", description = "Ruta za upravljanje primaocima plaćanja")
public class ReceiverController {

    private final ReceiverService receiverService;

    @Operation(
            summary = "Dodavanje novog primaoca",
            description = "Dodaje novog primaoca u listu primaoca plaćanja za određeni bankovni nalog."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primalac uspešno dodat",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"message\": \"Primalac uspešno dodat\", \"data\": { \"id\": 1, \"ownerAccountId\": 2, \"accountNumber\": \"123456789\", \"firstName\": \"Petar\", \"lastName\": \"Petrović\", \"address\": \"Bulevar Kralja Aleksandra 23\" } }"))
            ),
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Nevalidni podaci.\" }"))
            )
    })
    @PostMapping
    @ReceiverAuthorization
    public ResponseEntity<?> addReceivers(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci o primaocu",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReceiverDTO.class),
                            examples = @ExampleObject(value = "{ \"ownerAccountId\": 2, \"accountNumber\": \"123456789\", \"fullName\": \"Petar Petrović\", \"address\": \"Bulevar Kralja Aleksandra 23\" }"))
            ) ReceiverDTO receiverDTO) {
        try {
            Receiver receiver = receiverService.createReceiver(receiverDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Primalac uspešno dodat", "data", receiver), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @Operation(
            summary = "Dohvatanje liste primaoca za određeni bankovni nalog",
            description = "Vraća listu svih primaoca plaćanja za određeni bankovni nalog korisnika."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista primaoca uspešno dohvaćena",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"receivers\": [ { \"id\": 1, \"ownerAccountId\": 2, \"accountNumber\": \"123456789\", \"firstName\": \"Petar\", \"lastName\": \"Petrović\", \"address\": \"Bulevar Kralja Aleksandra 23\" } ] }"))
            ),
            @ApiResponse(responseCode = "404", description = "Nema primaoca za dati nalog",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Nema sačuvanih primaoca za ovaj nalog.\" }"))
            )
    })
    @GetMapping("/{accountId}")
    @ReceiverAuthorization
    public ResponseEntity<?> getReceivers(
            @Parameter(description = "ID naloga korisnika", required = true, example = "2")
            @PathVariable Long accountId) {
        if (!receiverService.accountExists(accountId)) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Nalog ne postoji.");
        }
        List<Receiver> receivers = receiverService.getReceiversByAccountId(accountId);
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("receivers", receivers), null);
    }

    @Operation(
            summary = "Ažuriranje informacija o primaocu",
            description = "Menja podatke o primaocu za dati ID (ime, prezime, broj računa, adresu)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primalac uspešno ažuriran",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"message\": \"Primalac uspešno ažuriran\", \"receiver\": { \"id\": 1, \"ownerAccountId\": 2, \"accountNumber\": \"987654321\", \"firstName\": \"Nikola\", \"lastName\": \"Nikolić\", \"address\": \"Nemanjina 4\" } }"))
            ),
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili primalac ne postoji",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Primalac ne postoji ili su podaci nevalidni.\" }"))
            )
    })
    @PutMapping("/{id}")
    @ReceiverAuthorization
    public ResponseEntity<?> updateReceiver(
            @Parameter(description = "ID primaoca", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci za ažuriranje primaoca",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReceiverDTO.class),
                            examples = @ExampleObject(value = "{ \"ownerAccountId\": 2, \"accountNumber\": \"987654321\", \"fullName\": \"Nikola Nikolić\", \"address\": \"Nemanjina 4\" }"))
            ) ReceiverDTO receiverDTO) {
        try {
            Receiver updatedReceiver = receiverService.updateReceiver(id, receiverDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Primalac uspešno ažuriran", "receiver", updatedReceiver), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @Operation(
            summary = "Brisanje primaoca iz liste",
            description = "Briše primaoca iz liste primaoca plaćanja na osnovu ID-ja."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primalac uspešno obrisan",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": true, \"message\": \"Primalac uspešno obrisan\" }"))
            ),
            @ApiResponse(responseCode = "404", description = "Primalac nije pronađen",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"success\": false, \"error\": \"Primalac nije pronađen.\" }"))
            )
    })
    @DeleteMapping("/{id}")
    @ReceiverAuthorization
    public ResponseEntity<?> deleteReceiver(
            @Parameter(description = "ID primaoca", required = true, example = "1")
            @PathVariable("id") Long receiverId) {
        receiverService.deleteReceiver(receiverId);
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Primalac uspešno obrisan"), null);
    }
}

