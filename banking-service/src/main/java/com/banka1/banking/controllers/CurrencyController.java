package com.banka1.banking.controllers;

import com.banka1.banking.dto.ExchangePairDTO;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.services.CurrencyService;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/currency")
@Tag(name = "Currency API", description = "API za upravljanje valutama i kursnim listama")
public class CurrencyController {
    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping("/available")
    @Operation(summary = "Dohvatanje dostupnih valuta", description = "Vraća listu podržanih valuta u sistemu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista dostupnih valuta", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "data": {
                    "currencies": [
                      "RSD",
                      "EUR",
                      "USD",
                      "GBP",
                      "CHF",
                      "JPY",
                      "CAD",
                      "AUD"
                    ]
                  },
                  "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "500", description = "Interna greška servera", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Internal server error."
                }
            """))
        )
    })
    public ResponseEntity<?> getAvailableCurrencies() {
        List<String> currencies = currencyService.getAvailableCurrencies();
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("currencies", currencies), null);
    }

    @GetMapping("/exchange-rates")
    @Operation(summary = "Dohvatanje kursne liste", description = "Vraća kursnu listu sa svim valutnim parovima.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista kursnih parova", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "data": {
                    "rates": [
                      {
                        "baseCurrency": "RSD",
                        "quoteCurrency": "EUR",
                        "rate": 0.0085,
                        "date": "2021-09-01"
                      }
                    ]
                  },
                  "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "500", description = "Interna greška servera", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Internal server error."
                }
            """))
        )
    })
    public ResponseEntity<?> getAllExchangeRates() {
        List<ExchangePairDTO> rates = currencyService.getAllExchangeRates();
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("rates", rates), null);
    }

    @GetMapping("/exchange-rates/{currency}")
    @Operation(summary = "Dohvatanje kursne liste za odabranu baznu valutu", description = "Vraća kursnu listu sa odabranom baznom valutom.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista kursnih parova za odabranu baznu valutu", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "data": {
                    "rates": [
                      {
                        "baseCurrency": "RSD",
                        "quoteCurrency": "EUR",
                        "rate": 0.0085,
                        "date": "2021-09-01"
                      }
                    ]
                  },
                  "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Ne postoji kursna lista za traženu valutu", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Ne postoji kursna lista za traženu valutu RSD"
                }
            """))
        ),
        @ApiResponse(responseCode = "500", description = "Interna greška servera", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Internal server error."
                }
            """))
        )
    })
    public ResponseEntity<?> getExchangeRatesForCurrency(@PathVariable CurrencyType currency) {
        List<ExchangePairDTO> rates = currencyService.getExchangeRatesForBaseCurrency(currency);
        if (rates.isEmpty()) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                    "Ne postoji kursna lista za traženu valutu " + currency);
        }
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("rates", rates), null);
    }
}
