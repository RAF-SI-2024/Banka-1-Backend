package com.banka1.banking.controllers;

import com.banka1.banking.aspect.CardAuthorization;
import com.banka1.banking.dto.CreateCardDTO;
import com.banka1.banking.dto.UpdateCardDTO;
import com.banka1.banking.dto.UpdateCardLimitDTO;
import com.banka1.banking.dto.request.UpdateCardNameDTO;
import com.banka1.banking.models.Card;
import com.banka1.banking.services.CardService;
import com.banka1.banking.utils.ResponseMessage;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cards")
@Tag(name = "Card API", description = "API za upravljanje karticama")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/{account_id}")
    @Operation(summary = "Pregled svih kartica za traženi račun", description = "Pregled svih kartica korisnika za traženi račun")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista kartica korisnika za određeni račun.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "data": {
                     "cards": [
                       {
                         "id": 1,
                         "cardNumber": "4971264106547603",
                         "cardName": "STANDARD kartica",
                         "cardBrand": "VISA",
                         "cardType": "DEBIT",
                         "cardCvv": "248",
                         "account": {
                           "id": 1,
                           "ownerID": 1,
                           "accountNumber": "111000100000000299",
                           "balance": 10000000,
                           "reservedBalance": 0,
                           "type": "BANK",
                           "currencyType": "EUR",
                           "subtype": "STANDARD",
                           "createdDate": 2025030500000,
                           "expirationDate": 2029030500000,
                           "dailyLimit": 1000000,
                           "monthlyLimit": 1000000,
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
                         "createdAt": 1742750467,
                         "expirationDate": 1900516867,
                         "active": true,
                         "blocked": false,
                         "cardLimit": 1000000,
                         "authorizedPerson": null
                       }
                     ]
                   },
                   "success": true
                 }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nedovoljna autorizacija."
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Nema kartica za traženi račun.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nema kartica za traženi račun."
                }
            """))
        )
    })
    @CardAuthorization
    public ResponseEntity<?> getCardsByAccountID(@PathVariable("account_id") int accountId) {
        return getCards(accountId);
    }

    @PostMapping("/")
    @Operation(summary = "Kreiranje kartice", description = "Kreira novu karticu povezanu sa računom korisnika.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Kartica uspešno kreirana.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "id": 1,
                    "message": "Kartica uspešno kreirana."
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nedovoljna autorizacija."
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili račun nije pronađen.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nevalidni podaci ili račun nije pronađen."
                }
            """))
        )
    })
    @CardAuthorization
    public ResponseEntity<?> createCard(@RequestBody CreateCardDTO createCardDTO) {
        try {
            Card card = cardService.createCard(createCardDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, Map.of("id", card.getId(), "message", ResponseMessage.CARD_CREATED_SUCCESS.toString()), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom kreiranja kartice: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PostMapping("/{card_id}")
    @Operation(summary = "Blokiranje i deblokiranje kartice", description = "Blokiranje i deblokiranje kartice")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Kartica uspešno ažurirana", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Kartica uspešno ažurirana"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili kartica nije pronađena.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nevalidni podaci ili kartica nije pronađena."
                }
            """))
        )
    })
    @CardAuthorization
    public ResponseEntity<?> blockCard(@PathVariable("card_id") int cardId, @RequestBody UpdateCardDTO updateCardDTO) {
        try {
            cardService.blockCard(cardId, updateCardDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", ResponseMessage.CARD_UPDATED_SUCCESS.toString()), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom ažuriranja kartice: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/admin/{account_id}")
    @Operation(summary = "Pregled svih kartica od strane zaposlenog", description = "Pregled svih kartica za traženi račun od strane zaposlenog")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista kartica korisnika za određeni račun.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "data": {
                     "cards": [
                       {
                         "id": 1,
                         "cardNumber": "4971264106547603",
                         "cardName": "STANDARD kartica",
                         "cardBrand": "VISA",
                         "cardType": "DEBIT",
                         "cardCvv": "248",
                         "account": {
                           "id": 1,
                           "ownerID": 1,
                           "accountNumber": "111000100000000299",
                           "balance": 10000000,
                           "reservedBalance": 0,
                           "type": "BANK",
                           "currencyType": "EUR",
                           "subtype": "STANDARD",
                           "createdDate": 2025030500000,
                           "expirationDate": 2029030500000,
                           "dailyLimit": 1000000,
                           "monthlyLimit": 1000000,
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
                         "createdAt": 1742750467,
                         "expirationDate": 1900516867,
                         "active": true,
                         "blocked": false,
                         "cardLimit": 1000000,
                         "authorizedPerson": null
                       }
                     ]
                   },
                   "success": true
                 }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nedovoljna autorizacija.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nedovoljna autorizacija."
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Nema kartica za traženi račun.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nema kartica za traženi račun."
                }
            """))
        )
    })
    @CardAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getAdminCardsByAccountID(@PathVariable("account_id") int accountId) {
        return getCards(accountId);
    }

    @PostMapping("/admin/{card_id}")
    @Operation(summary = "Aktivacija i deaktivacija kartice od strane zaposlenog", description = "Aktivacija i deaktivacija kartice od strane zaposlenog")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Kartica uspešno ažurirana", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Kartica uspešno ažurirana"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili nije moguće aktivirati.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Kartica je deaktivirana i ne moze biti aktivirana"
                }
            """))
        )
    })
    @CardAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> activateCard(@PathVariable("card_id") int cardId, @RequestBody UpdateCardDTO updateCardDTO) {
        try {
            cardService.activateCard(cardId, updateCardDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", ResponseMessage.CARD_UPDATED_SUCCESS.toString()), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom ažuriranja kartice: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    private ResponseEntity<?> getCards(int account_id) {
        try {
            List<Card> cards = cardService.findAllByAccountId(account_id);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("cards", cards), null);
        } catch (Exception e) {
            log.error("Greška prilikom trazenja kartica: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PostMapping("/{card_id}/limit")
    @Operation(summary = "Promena limita kartice", description = "Omogućava korisniku da promeni limit kartice.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Limit kartice uspešno promenjen.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Limit kartice uspešno ažuriran."
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili kartica nije pronađena.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nevalidni podaci ili kartica nije pronađena."
                }
            """))
        )
    })
    @CardAuthorization
    public ResponseEntity<?> updateCardLimit(@PathVariable("card_id") Long cardId, @RequestBody UpdateCardLimitDTO updateCardLimitDTO) {
        try {
            cardService.updateCardLimit(cardId, updateCardLimitDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Limit kartice uspešno ažuriran."), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom ažuriranja limita kartice: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PostMapping("/{card_id}/name")
    @Operation(summary = "Promena naziva kartice", description = "Omogućava korisniku da promeni naziv kartice.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Naziv kartice uspešno promenjen.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Naziv kartice uspešno ažuriran."
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Nevalidni podaci ili kartica nije pronađena.", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nevalidni podaci ili kartica nije pronađena."
                }
            """))
        )
    })
    @CardAuthorization
    public ResponseEntity<?> updateCardName(@PathVariable("card_id") Long cardId, @RequestBody UpdateCardNameDTO updateCardNameDTO) {
        try {
            cardService.updateCardName(cardId, updateCardNameDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Naziv kartice uspešno ažuriran."), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom ažuriranja naziva kartice: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
