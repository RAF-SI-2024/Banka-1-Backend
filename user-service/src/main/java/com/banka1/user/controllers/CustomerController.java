package com.banka1.user.controllers;

import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.request.UpdateCustomerRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.model.Customer;
import com.banka1.common.model.Permission;
import com.banka1.user.service.CustomerService;
import com.banka1.user.service.AuthService;
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

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer API", description = "API za upravljanje mušterijama")
public class CustomerController {

    private final CustomerService customerService;
    private final AuthService authService;

    public CustomerController(CustomerService customerService, AuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    @Operation(
        summary = "Dobavljanje informacija o mušteriji datog ID-a"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Uspešno dobijene informacije o mušteriji", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "data": {
                    "id": 1,
                    "firstName": "Pera",
                    "lastName": "Petrovic",
                    "username": "pera123",
                    "birthDate": "2002-12-12",
                    "gender": "MALE",
                    "email": "pera@banka.com",
                    "phoneNumber": "+38160123123",
                    "address": "Knez Mihailova 6",
                    "permissions": [
                      "user.employee.view"
                    ]
                  },
                  "success": true
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Nedovoljna autorizacija.",
                  "success": false
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Neispravni podaci ili korisnik ne postoji", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik nije pronađen."
                }
            """))
        )
    })
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
                        false, null, "Korisnik nije pronađen.");
            return ResponseTemplate.create(ResponseEntity.ok(), true, customer, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PostMapping
    @Authorization(permissions = { Permission.CREATE_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Kreiranje mušterije", description = "Kreira mušteriju i vraća ID kreirane mušterije")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mušterija uspešno kreirana", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "customer": {
                      "id": 7,
                      "firstName": "Pera",
                      "lastName": "Petrovic",
                      "birthDate": "2002-12-12",
                      "gender": "MALE",
                      "email": "pera@banka.com",
                      "username": "pera123",
                      "phoneNumber": "+38160123123",
                      "address": "Knez Mihailova 6",
                      "password": null,
                      "saltPassword": null,
                      "verificationCode": "6efb6106-9107-4f11-8baa-a2576f77dd04",
                      "permissions": null,
                      "bankAccounts": null
                    }
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom kreiranja mušterije", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Došlo je do greške prilikom kreiranja mušterije.",
                  "success": false
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija."
                 }
            """))
        )
    })
    public ResponseEntity<?> createCustomer(
            @RequestBody @Parameter(description = "Customer data for creation") CreateCustomerRequest customerDTO,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Customer savedCustomer = customerService.createCustomer(customerDTO, authService.parseToken(authService.getToken(authorization)).get("id", Long.class));
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, Map.of("customer", savedCustomer), null);
    }

    @PutMapping("/{id}")
    @Authorization(permissions = { Permission.EDIT_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Ažuriranje mušterije")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Podaci korisnika ažurirani", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Podaci korisnika ažurirani"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija."
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
    public ResponseEntity<?> updateCustomer(
            @PathVariable @Parameter(description = "ID musterije") Long id,
            @RequestBody UpdateCustomerRequest customerDTO) {
        Optional<Customer> updatedCustomer = customerService.updateCustomer(id, customerDTO);

        if (updatedCustomer.isPresent()) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Podaci korisnika ažurirani"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

    @DeleteMapping("/{id}")
    @Authorization(permissions = { Permission.DELETE_CUSTOMER }, allowIdFallback = true )
    @Operation(summary = "Brisanje mušterije")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Korisnik uspešno obrisan", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Korisnik uspešno obrisan"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija."
                 }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik nije pronađen"
                }
            """)))
    })
    public ResponseEntity<?> deleteCustomer(
            @PathVariable @Parameter(description = "ID musterije") Long id) {
        boolean deleted = customerService.deleteCustomer(id);

        if (deleted) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Korisnik uspešno obrisan"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

    @PutMapping("/{id}/permissions")
    @Authorization(permissions = { Permission.SET_CUSTOMER_PERMISSION }, allowIdFallback = true )
    @Operation(summary = "Ažuriranje permisija mušterije")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uspesno ažurirane permisije", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Permisije ažurirane"
                  }
                }
            """))
        ),
        @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": false,
                   "error": "Nedovoljna autorizacija."
                 }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik nije pronađen"
                }
            """)))
    })
    public ResponseEntity<?> updateCustomerPermissions(
            @PathVariable @Parameter(description = "ID musterije") Long id,
            @RequestBody UpdatePermissionsRequest permissionsDto) {
        Optional<Customer> updatedCustomer = customerService.updateCustomerPermissions(id, permissionsDto.getPermissions());

        if (updatedCustomer.isPresent()) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("message", "Permisije ažurirane"), null);
        } else {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Korisnik nije pronađen");
        }

    }

}
