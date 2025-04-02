package com.banka1.banking.controllers;

import com.banka1.banking.aspect.AccountAuthorization;
import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.dto.ExchangePreviewDTO;
import com.banka1.banking.services.ExchangeService;
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
@RequestMapping("/exchange-transfer")
@RequiredArgsConstructor
@Tag(name = "Menjačnica", description = "Ruta za upravljanje transferima sa konverzijom valute između računa istog korisnika")
public class ExchangeController {

    private final ExchangeService exchangeService;

    @Operation(summary = "Transfer sa konverzijom", description = "Izvršava transfer novca između različitih valuta za isti račun korisnika.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Interni prenos sa konverzijom uspešno izvršen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "success": true,
                    "data": {
                        "message": "Interni prenos sa konverzijom uspešno izvršen.",
                        "transferId": 12345
                    }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nedovoljno sredstava", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "success": false,
                    "error": "Nevalidni podaci ili nedovoljno sredstava."
                }
            """))
        )
    })
    @PostMapping
    @AccountAuthorization(customerOnlyOperation = true)
    public ResponseEntity<?> exchangeMoneyTransfer(
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Podaci za transfer sa konverzijom",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ExchangeMoneyTransferDTO.class),
                            examples = @ExampleObject(value = "{ \"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 500.0, \"fromCurrency\": \"EUR\", \"toCurrency\": \"USD\" }"))
            ) ExchangeMoneyTransferDTO exchangeMoneyTransferDTO) {

        // PROVERITI DA LI SE VALUTE SALJU U DTO
        try {
            if(!exchangeService.validateExchangeTransfer(exchangeMoneyTransferDTO)){
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST),
                        false,null,"Nevalidni podaci ili nedovoljno sredstava.");
            }

            Long transferId = exchangeService.createExchangeTransfer(exchangeMoneyTransferDTO);

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK),true, Map.of("message","Interni prenos sa konverzijom uspesno izvršen.","transferId",transferId),null);

        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }

    }

    @Operation(summary = "Pregled kursa pre razmene", description = "Vraća kurs, iznos nakon konverzije, proviziju i krajnji iznos pre nego što korisnik potvrdi transfer.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešno izračunata konverzija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "exchangeRate": 117.5,
                    "convertedAmount": 8.51,
                    "fee": 0.25,
                    "finalAmount": 8.26
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nepostojeći kurs", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "error": "Kurs nije pronađen za traženu konverziju."
                }
            """))
        )
    })
    @PostMapping("/preview")
    public ResponseEntity<?> previewExchange(@RequestBody ExchangePreviewDTO exchangePreviewDTO) {
        try {
            Map<String, Object> previewData = exchangeService.calculatePreviewExchange(
                    exchangePreviewDTO.getFromCurrency(),
                    exchangePreviewDTO.getToCurrency(),
                    exchangePreviewDTO.getAmount()
            );
            return ResponseEntity.ok(previewData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Pregled kursa za stranu valutu pre razmene", description = "Vraća kurs za obe strane valute, ukupnu proviziju i konačan iznos nakon oduzimanja provizije.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Uspešno izračunata konverzija strane valute", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "firstExchangeRate": 117.5,
                    "secondExchangeRate": 105.2,
                    "totalFee": 5.0,
                    "finalAmount": 95.0
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nepostojeći kurs", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "error": "Kurs nije pronađen za traženu konverziju."
                }
            """))
        )
    })
    @PostMapping("/preview-foreign")
    public ResponseEntity<?> previewExchangeForeign(@RequestBody ExchangePreviewDTO exchangePreviewDTO) {
        try {
            Map<String, Object> previewData = exchangeService.calculatePreviewExchangeForeign(
                    exchangePreviewDTO.getFromCurrency(),
                    exchangePreviewDTO.getToCurrency(),
                    exchangePreviewDTO.getAmount()
            );
            return ResponseEntity.ok(previewData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
