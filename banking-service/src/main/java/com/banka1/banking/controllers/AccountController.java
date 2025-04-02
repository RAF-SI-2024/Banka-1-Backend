package com.banka1.banking.controllers;

import com.banka1.banking.aspect.AccountAuthorization;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.dto.request.UpdateAccountDTO;
import com.banka1.banking.dto.request.UserUpdateAccountDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.services.AccountService;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.banking.utils.ResponseTemplate;
import com.banka1.banking.utils.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
@Tag(name = "Account API", description = "API za upravljanje racunima")
public class AccountController {
    private final AccountService accountService;
    private final AuthService authService;

    /// pristup imaju samo zaposleni
    /// Da bi zaposleni mogao da kreira novi račun, potrebno je da se prijavi u aplikaciju.
    /// Pored čuvanja podataka o vlasniku (klijentu), čuvaju se i podaci o zaposlenima koji su napravili račune.
    /// Nakon prijave, izabira jedan od tipova računa: Tekući račun, Devizni račun. Nakon uspešnog kreiranog računa vlasnik dobija email o uspehu.
    @PostMapping("/")
    @Operation(summary = "Kreiranje računa", description = "Dodaje novi račun u sistem.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Račun uspešno kreiran.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "account_id": "111000141497202317",
                     "message": "Račun uspešno kreiran."
                   }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija"
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Korisnik nije pronađen"
                }
            """))
        )
    })
    @AccountAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountDTO createAccountDTO, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Account savedAccount;
        try {
            savedAccount = accountService.createAccount(createAccountDTO, authService.parseToken(authService.getToken(authorization)).get("id", Long.class));
        } catch (RuntimeException e) {

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("account_id", savedAccount.getAccountNumber());
        response.put("message", "Račun uspešno kreiran.");

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, response, null);
    }

    /// pristup imaju samo zaposleni
    @GetMapping("/")
    @Operation(summary = "Dohvatanje svih računa", description = "Vraća listu svih računa u sistemu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista računa uspešno dohvaćena", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                      "accounts": [
                        {
                         "id": 1,
                         "ownerID": 1,
                         "accountNumber": "111000100000000199",
                         "balance": 10000000000,
                         "reservedBalance": 0,
                         "type": "BANK",
                         "currencyType": "RSD",
                         "subtype": "STANDARD",
                         "createdDate": 2025030500000,
                         "expirationDate": 2029030500000,
                         "dailyLimit": 10000000,
                         "monthlyLimit": 100000000,
                         "dailySpent": 0,
                         "monthlySpent": 0,
                         "status": "ACTIVE",
                         "employeeID": 1,
                         "monthlyMaintenanceFee": 0,
                         "company": {
                           "id": 1,
                           "name": "Naša Banka",
                           "address": "Bulevar Banka 1",
                           "vatNumber": "111111111",
                           "companyNumber": "11111111"
                         }
                       }
                     ]
                   }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija"
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Nema dostupnih računa", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Korisnik nema otvorenih racuna"
                }
            """))
        )
    })
    @AccountAuthorization
    public ResponseEntity<?> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();

        Map<String, Object> response = new HashMap<>();
        response.put("accounts", accounts);

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, response, null);
    }

    /// pristup imaju zaposleni i vlasnici racuna
    @GetMapping("/user/{userId}")
    @Operation(summary = "Dohvatanje računa specificnog korisnika", description = "Vraća sve račune vezane za određenog korisnika.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Računi uspešno dohvaćeni", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "accounts": [
                       {
                         "id": 1,
                         "ownerID": 1,
                         "accountNumber": "111000100000000110",
                         "balance": 100000,
                         "reservedBalance": 0,
                         "type": "CURRENT",
                         "currencyType": "RSD",
                         "subtype": "STANDARD",
                         "createdDate": 2025030500000,
                         "expirationDate": 1630454400000,
                         "dailyLimit": 10000,
                         "monthlyLimit": 100000,
                         "dailySpent": 0,
                         "monthlySpent": 0,
                         "status": "ACTIVE",
                         "employeeID": 1,
                         "monthlyMaintenanceFee": 0,
                         "company": null
                       }
                     ]
                   }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija"
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Korisnik ne postoji", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Korisnik nema otvorenih racuna"
                }
            """))
        )
    })
    @AccountAuthorization
    public ResponseEntity<?> getAccountsByOwner(@PathVariable Long userId) {

        List<Account> accounts = accountService.getAccountsByOwnerId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("accounts", accounts);

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, response, null);
    }

    /// pristup imaju samo zaposleni
    @PutMapping("/{accountId}")
    @Operation(summary = "Ažuriranje računa", description = "Omogućava zaposlenima da ažuriraju podatke o računu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Račun uspešno ažuriran", content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                       "success": true,
                       "data": {
                         "message": "UPDATED",
                         "data": {
                           "id": 1,
                       "ownerID": 7,
                       "accountNumber": "111000100000000199",
                       "balance": 10000000000,
                       "reservedBalance": 0,
                       "type": "BANK",
                       "currencyType": "RSD",
                       "subtype": "STANDARD",
                       "createdDate": 2025030500000,
                       "expirationDate": 2029030500000,
                       "dailyLimit": 2340,
                       "monthlyLimit": 0,
                       "dailySpent": 0,
                       "monthlySpent": 0,
                       "status": "ACTIVE",
                       "employeeID": 1,
                       "monthlyMaintenanceFee": 0,
                       "company": {
                         "id": 1,
                         "name": "Naša Banka",
                         "address": "Bulevar Banka 1",
                         "vatNumber": "111111111",
                         "companyNumber": "11111111"
                       }
                     }
                   }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija"
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci za ažuriranje ili korisnik nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nevalidni podaci za ažuriranje ili korisnik nije pronađen"
                }
            """))
        )
    })
    @AccountAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> updateAccount(
            @PathVariable Long accountId,
            @RequestBody UpdateAccountDTO updateAccountDTO) {

        try {
            Account updatedAccount = accountService.updateAccount(accountId, updateAccountDTO);
            return ResponseTemplate.create(ResponseEntity.ok(), true,
                    Map.of( "message", ResponseMessage.UPDATED, "data", updatedAccount), null);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    /// pristup imaju zaposleni i vlasnici racuna
    @PutMapping("/user/{userId}/{accountId}")
    @Operation(summary = "Ažuriranje računa od strane korisnika", description = "Omogućava korisnicima da ažuriraju podatke o računu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Račun uspešno ažuriran", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "data": {
                     "data": {
                       "id": 1,
                       "ownerID": 7,
                       "accountNumber": "111000100000000199",
                       "balance": 10000000000,
                       "reservedBalance": 0,
                       "type": "BANK",
                       "currencyType": "RSD",
                       "subtype": "STANDARD",
                       "createdDate": 2025030500000,
                       "expirationDate": 2029030500000,
                       "dailyLimit": 23540,
                       "monthlyLimit": 64560,
                       "dailySpent": 0,
                       "monthlySpent": 0,
                       "status": "ACTIVE",
                       "employeeID": 1,
                       "monthlyMaintenanceFee": 0,
                       "company": {
                         "id": 1,
                         "name": "Naša Banka",
                         "address": "Bulevar Banka 1",
                         "vatNumber": "111111111",
                         "companyNumber": "11111111"
                       }
                     },
                     "message": "UPDATED"
                   },
                   "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci za ažuriranje", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "error": "Nevalidni podaci za ažuriranje",
                   "success": false
                }
            """))
        )
    })
    public ResponseEntity<?> updateUserAccount(
            @PathVariable Long userId,
            @PathVariable Long accountId,
            @RequestBody UserUpdateAccountDTO updateAccountDTO) {

        try {
            Account updatedAccount = accountService.userUpdateAccount(userId, accountId, updateAccountDTO);
            if (updatedAccount == null) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false,
                        Map.of( "message", ResponseMessage.NOT_THE_OWNER), null);

            }
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true,
                    Map.of( "message", ResponseMessage.UPDATED, "data", updatedAccount), null);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Dohvatanje transakcija za izabrani račun", description = "Vraća sve transakcije za izabrani račun.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transakcije uspešno dohvaćene", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "transactions": [
                       {
                         "id": 1,
                         "fromAccountId": 1,
                         "toAccountId": 2,
                         "amount": 1000,
                         "currencyType": "RSD",
                         "transactionType": "TRANSFER",
                         "transactionDate": 2025030500000,
                         "status": "COMPLETED"
                       }
                     ]
                   }
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Račun nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nema transakcija za izabrani racun"
                }
            """))
        )
    })
    @AccountAuthorization
    public ResponseEntity<?> getTransactionsForAccount(@PathVariable Long accountId) {

        // Proveri da li račun postoji
        if (accountService.findById(accountId) == null) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Račun sa ID-jem " + accountId + " nije pronađen.");
        }

        List<Transaction> transactions = accountService.getTransactionsForAccount(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, response, null);
    }

}
