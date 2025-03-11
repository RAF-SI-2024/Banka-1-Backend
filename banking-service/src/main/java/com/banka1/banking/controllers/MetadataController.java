package com.banka1.banking.controllers;

import com.banka1.banking.models.helper.PaymentType;
import com.banka1.banking.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metadata")
@Tag(name = "Banking Metadata", description = "API za upravljanje metapodacima aplikacije")
public class MetadataController {
    @GetMapping("/payment-codes")
    @Operation(summary = "Dobavljanje svih kodova plaćanja", description = "Vraća listu svih kodova plaćanja u sistemu.")
    @ApiResponse(responseCode = "200", description = "Uspešno dobavljanje kodova plaćanja", content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{ \"codes\": [ { \"code\": \"254\", \"description\": \"Uplata poreza i doprinosa po odbitku\" }, { \"code\": \"283\", \"description\": \"Naplata čekova građana\" }, { \"code\": \"284\", \"description\": \"Platne kartice\" }, { \"code\": \"288\", \"description\": \"Donacije donacije iz međunarodnih ugovora\" }, { \"code\": \"289\", \"description\": \"Transakcije po nalogu građana\" }, { \"code\": \"290\", \"description\": \"Druge transakcije\" } ] }"

    )))
    public ResponseEntity<?> getAllPaymentCodes() {
        List<PaymentType> paymentTypes = new ArrayList<>();
        paymentTypes.add(new PaymentType("254", "Uplata poreza i doprinosa po odbitku"));
        paymentTypes.add(new PaymentType("283", "Naplata čekova građana"));
        paymentTypes.add(new PaymentType("284", "Platne kartice"));
        paymentTypes.add(new PaymentType("288", "Donacije donacije iz međunarodnih ugovora"));
        paymentTypes.add(new PaymentType("289", "Transakcije po nalogu građana"));
        paymentTypes.add(new PaymentType("290", "Druge transakcije"));

        return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("codes", paymentTypes), null);
    }
}
