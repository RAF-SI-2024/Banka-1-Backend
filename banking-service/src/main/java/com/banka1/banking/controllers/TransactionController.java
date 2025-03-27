package com.banka1.banking.controllers;

import com.banka1.banking.aspect.AccountAuthorization;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.services.TransactionService;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction API", description = "API za upravljanje transakcijama")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AuthService authService;

    @GetMapping("/{userId}")
    @Operation(summary = "Dobavljanje svih transakcija", description = "Vraća listu svih transakcija u sistemu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uspešno dobavljanje transakcija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value =
                """
                    {
                      "data": [
                        {
                          "id": 1,
                          "fromAccount": "1",
                          "toAccount": "2",
                          "amount": 1000,
                          "currency": "RSD",
                          "timestamp": 202106012,
                          "description": "Opis transakcije",
                          "loanId": 1,
                          "transfer": {
                            "id": 1,
                            "fromAccount": "1",
                            "toAccount": "2",
                            "amount": 1000,
                            "receiver": "Ime Prezime",
                            "address": "Adresa",
                            "paymentCode": "123456789",
                            "paymentReference": "123456789",
                            "paymentDescription": "Opis uplate",
                            "from_currency": "RSD",
                            "to_currency": "EUR",
                            "created_at": 202106012,
                            "otp": "123456",
                            "type": "INTERNAL",
                            "status": "PENDING",
                            "completedAt": 202106012,
                            "note": "Napomena"
                          }
                        }
                      ],
                      "success": true
                    }
                """))
        ),
        @ApiResponse(responseCode = "400", description = "Greška pri dobavljanju transakcija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value =
                """
                    {
                      "success": false,
                      "error": "Greška pri dobavljanju transakcija."
                    }
                """))
        )
    })
    @AccountAuthorization
    public ResponseEntity<?> getAllTransactions(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long userId) {
        try {
            if (!transactionService.userExists(userId)) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik ne postoji.");
            }
            List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", transactions);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, response, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/admin/{userId}")
    @Operation(summary = "Dobavljanje svih transakcija za korisnika", description = "Vraća listu svih transakcija za korisnika sa datim ID-em.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uspešno dobavljanje transakcija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "data": [
                    {
                      "id": 1,
                      "fromAccount": "1",
                      "toAccount": "2",
                      "amount": 1000,
                      "currency": "RSD",
                      "timestamp": 202106012,
                      "description": "Opis transakcije",
                      "loanId": 1,
                      "transfer": {
                        "id": 1,
                        "fromAccount": "1",
                        "toAccount": "2",
                        "amount": 1000,
                        "receiver": "Ime Prezime",
                        "address": "Adresa",
                        "paymentCode": "123456789",
                        "paymentReference": "123456789",
                        "paymentDescription": "Opis uplate",
                        "from_currency": "RSD",
                        "to_currency": "EUR",
                        "created_at": 202106012,
                        "otp": "123456",
                        "type": "INTERNAL",
                        "status": "PENDING",
                        "completedAt": 202106012,
                        "note": "Napomena"
                      }
                    }
                  ],
                  "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Greška pri dobavljanju transakcija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Greška pri dobavljanju transakcija."
                }
            """))
        )
    })
    @AccountAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getAllTransactionsAdmin(@PathVariable Long userId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", transactions);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, response, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
