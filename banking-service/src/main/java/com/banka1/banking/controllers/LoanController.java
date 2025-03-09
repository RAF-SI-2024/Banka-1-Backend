package com.banka1.banking.controllers;

import com.banka1.banking.aspect.AccountAuthorization;
import com.banka1.banking.aspect.CardAuthorization;
import com.banka1.banking.dto.request.CreateAccountDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.dto.request.UserUpdateAccountDTO;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Card;
import com.banka1.banking.models.Loan;
import com.banka1.banking.services.LoanService;
import com.banka1.banking.services.implementation.AuthService;
import com.banka1.banking.utils.ResponseMessage;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loan API", description = "API za pozive vezane za kredite i rate")
public class LoanController {
    @Autowired
    private LoanService loanService;
    @Autowired
    private AuthService authService;

    @PostMapping("/")
    @Operation(summary = "Kreiranje zahteva za kredit",
            description = "Dodaje novi kredit u sistem i cuva se u bazi pod statusom na cekanju.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kredit uspešno kreiran.\n"),
            @ApiResponse(responseCode = "403", description = "Nevalidni podaci")
    })
    public ResponseEntity<?> createLoan(@Valid @RequestBody CreateLoanDTO createLoanDTO) {
        Loan newLoan = null;
        try {
            newLoan = loanService.createLoan(createLoanDTO);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }

        if (newLoan == null) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.SEE_OTHER), false, null, ResponseMessage.WRONG_NUM_OF_INSTALLMENTS.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("broj racuna", newLoan.getId());
        response.put("message", "Kredit uspešno kreiran.\n");

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, response, null);
    }

/// samo zaposleni imaju pristup
    @GetMapping("/pending")
    @Operation(summary = "Pregled svih kredita na cekanju",
            description = "Pregled svih kredita za koje su korisnici podneli zahtev a koji jos uvek nisu odobreni/odbijeni")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "lista kredita."),
            @ApiResponse(responseCode = "404", description = "nema kredita na cekanju.")
    })
//    @Authorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getPendingLoans() {
        try {
            List<Loan> loans = loanService.getPendingLoans();
            if (loans.isEmpty()) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, ResponseMessage.NO_DATA.toString());
            }
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("loans", loans), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    /// samo zaposleni imaju pristup
    @GetMapping("/admin/{user_id}")
    @Operation(summary = "Pregled svih kredita korisnika",
            description = "Pregled svih kredita koje korisnik ima na racunima")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "lista kredita."),
            @ApiResponse(responseCode = "404", description = "nema kredita na cekanju.")
    })
//    @Authorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getAllUserLoans(
            @PathVariable("user_id") Long ownerId) {
        try {
            List<Loan> loans = loanService.getAllUserLoans(ownerId);
            if (loans == null) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                        ResponseMessage.ACCOUNTS_NOT_FOUND.toString());
            }
            else if (loans.isEmpty()) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                        ResponseMessage.NO_DATA.toString());
            }
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("loans", loans), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @GetMapping("/")
    @Operation(summary = "Pregled svih kredita korisnika",
            description = "Pregled svih kredita korisnika")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "lista kredita."),
            @ApiResponse(responseCode = "404", description = "nema kredita na cekanju.")
    })
    public ResponseEntity<?> getLoansByUserId(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            Long ownerId = 3L;//authService.parseToken(authService.getToken(authorization)).get("id", Long.class);
            List<Loan> loans = loanService.getAllUserLoans(ownerId);
            if (loans == null) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                        ResponseMessage.ACCOUNTS_NOT_FOUND.toString());
            }
            else if (loans.isEmpty()) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                        ResponseMessage.NO_DATA.toString());
            }
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("loans", loans), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @GetMapping("/{loan_id}")
    @Operation(summary = "Pregled svih kredita korisnika",
            description = "Pregled svih kredita korisnika")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "lista kredita."),
            @ApiResponse(responseCode = "404", description = "nema kredita na cekanju.")
    })
    public ResponseEntity<?> getLoanDetails(
            @PathVariable("loan_id") Long loanId
            /*, @RequestHeader(value = "Authorization", required = false) String authorization*/) {
        try {
            Long ownerId = 3L;//authService.parseToken(authService.getToken(authorization)).get("id", Long.class);
            Loan loan = loanService.getLoanDetails(ownerId, loanId);
            if (loan == null) {
                return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null,
                        ResponseMessage.INVALID_REQUEST.toString());
            }
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("loan", loan), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

/// samo zaposleni ima pristup
    @PutMapping("/admin/{loan_id}/approve")
    @Operation(summary = "Ažuriranje zahteva za kredit od strane zaposlenog",
            description = "Omogućava zaposlenima da ažuriraju stanje kredita.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kredit uspešno ažuriran"),
            @ApiResponse(responseCode = "404", description = "Kredit nije pronađen"),
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci za ažuriranje")
    })
    //    @Authorization(employeeOnlyOperation = true)
    public ResponseEntity<?> updateLoanRequest(
            @PathVariable("loan_id") Long loanId,
            @RequestBody LoanUpdateDTO loanUpdateDTO) {
        try {
            Loan updatedLoan = loanService.updateLoanRequest(loanId, loanUpdateDTO);
            if (updatedLoan == null) {
                return ResponseTemplate.create(ResponseEntity.badRequest(), false,
                        Map.of( "message", ResponseMessage.INVALID_REQUEST), null);
            }
            return ResponseTemplate.create(ResponseEntity.ok(), true,
                    Map.of( "message", ResponseMessage.UPDATED, "data", updatedLoan), null);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

}
