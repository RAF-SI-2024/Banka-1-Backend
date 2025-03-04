package com.banka1.user.controllers;

import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.DTO.request.UpdateCustomerRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.service.CustomerService;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Musterije")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(
            summary = "Dobavljanje informacija o musteriji datog ID-a"
    )
    @GetMapping("/{id}")
    @Authorization(permissions = { Permission.READ_CUSTOMER }, allowIdFallback = true )
    public ResponseEntity<?> getById(
            @Parameter(required = true, example = "1")
            @PathVariable String id
    ) {
        try {
            var customer = customerService.findById(id);
            if (customer == null)
                return ResponseTemplate.create(ResponseEntity.status(HttpStatusCode.valueOf(404)),
                        false, null, "Korisnik nije pronadjen.");
            return ResponseTemplate.create(ResponseEntity.ok(), true, customer, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @PostMapping
    @Authorization(permissions = { Permission.CREATE_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Kreiranje musterije", description = "Kreira musteriju i vraca ID kreirane musterije")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zaposleni uspešno kreiran", content = @Content(examples = {@ExampleObject(description = "{sucess: true, data: 1}")}) ),
            @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju")
    })
    public ResponseEntity<?> createCustomer(
            @RequestBody @Parameter(description = "Customer data for creation") CreateCustomerRequest customerDTO) {
        Customer savedCustomer = customerService.createCustomer(customerDTO);
        return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("id", savedCustomer.getId()), null);
    }

    @PutMapping("/{id}")
    @Authorization(permissions = { Permission.EDIT_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Promena musterije")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uspesna promena"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronadjen")
    })
    public ResponseEntity<?> updateCustomer(
            @PathVariable @Parameter(description = "ID musterije") Long id,
            @RequestBody UpdateCustomerRequest customerDTO) {
        Optional<Customer> updatedCustomer = customerService.updateCustomer(id, customerDTO);

        if (updatedCustomer.isPresent()) {
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", "Podaci korisnika ažurirani"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

    @DeleteMapping("/{id}")
    @Authorization(permissions = { Permission.DELETE_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Brisanje musterije")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uspesno brisanje"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronadjen")
    })
    public ResponseEntity<?> deleteCustomer(
            @PathVariable @Parameter(description = "ID musterije") Long id) {
        boolean deleted = customerService.deleteCustomer(id);

        if (deleted) {
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", "Korisnik uspešno obrisan"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

    @PutMapping("/{id}/permissions")
    @Authorization(permissions = { Permission.SET_CUSTOMER_PERMISSION }, allowIdFallback = true )
    @Operation(summary = "Promena permisija musterije")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspeh"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronadjen")
    })
    public ResponseEntity<?> updateCustomerPermissions(
            @PathVariable @Parameter(description = "ID musterije") Long id,
            @RequestBody UpdatePermissionsRequest permissionsDto) {
        Optional<Customer> updatedCustomer = customerService.updateCustomerPermissions(id, permissionsDto.getPermissions());

        if (updatedCustomer.isPresent()) {
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", "Permisije ažurirane"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

    @PutMapping("/set-password")
    @Operation(summary = "Postavljanje lozinke", description = "Postavljanje lozinke i validacija mejla nakon kreiranja musterije")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspeh"),
    })
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest setPasswordRequest) {
        try {
            customerService.setPassword(setPasswordRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", "Lozinka uspešno postavljena"), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }


}
