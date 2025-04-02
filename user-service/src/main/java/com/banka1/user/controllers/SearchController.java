package com.banka1.user.controllers;

import com.banka1.common.model.Permission;
import com.banka1.user.aspect.Authorization;
import com.banka1.common.model.Position;
import com.banka1.user.service.CustomerService;
import com.banka1.user.service.EmployeeService;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/search/")
@Tag(name = "Pretraga korisnika")
public class SearchController {
    private final CustomerService customerService;
    private final EmployeeService employeeService;

    @Operation(
            summary = "Pretraga svih zaposlenih",
            description = "Vraca sve zaposlene koji zadovoljavaju (opcioni) filter, sortirani po zadatom polju. Rezultati se vracaju podeljeni na stranice."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uspešna pretraga", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "total": 1,
                     "rows": [
                       {
                         "id": 5,
                         "firstName": "Pera",
                         "lastName": "Petrovic",
                         "username": "pera123",
                         "birthDate": "2000-10-10",
                         "gender": "MALE",
                         "email": "pera@banka.com",
                         "phoneNumber": "+381601001001",
                         "address": "Knez Mihailova 6",
                         "position": "WORKER",
                         "department": "AGENT",
                         "active": true,
                         "isAdmin": false,
                         "permissions": [
                           "user.customer.view"
                         ]
                       }
                     ]
                   }
                 }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Neuspešna pretraga", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Specificirano polje za filtriranje ali nije specificirana vrednost."
                }
            """))
        )
    })
    @GetMapping("employees")
    @Authorization(permissions = {Permission.LIST_EMPLOYEE})
    public ResponseEntity<?> searchEmployees(
            @Parameter(
                    description = "Redni broj stranice rezultata (podrazumevana vrednost je prva stranica)",
                    example = "0"
            )
            @RequestParam Optional<Integer> page,
            @Parameter(
                    description = "Velicina jedne stranice (podrazumevana vrednost je 10)",
                    example = "10"
            )
            @RequestParam Optional<Integer> pageSize,
            @Parameter(
                    description = "Polje po kojem se sortira. Ako nije postavljeno, sortira se po ID-u.",
                    example = "firstName"
            )
            @RequestParam Optional<String> sortField,
            @Parameter(
                    description = "Redosled sortiranja. Mora biti asc ili desc, podrazumeva se asc.",
                    example = "desc"
            )
            @RequestParam Optional<String> sortOrder,
            @Parameter(
                    description = "Polje po kojem se filtriraju rezultati. Nema filtera ako se ne postavi.",
                    example = "lastName"
            )
            @RequestParam Optional<String> filterField,
            @Parameter(
                    description = "Tekst koji se trazi u filtriranom polju. Mora se postaviti ako je postavljen filterField.",
                    example = "Petrovic"
            )
            @RequestParam Optional<String> filterValue
    ) {
        try {
            var employees = employeeService.search(
                    page.orElse(0), pageSize.orElse(10),
                    sortField, sortOrder, filterField, filterValue
            );
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, employees, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @Operation(
            summary = "Pretraga svih musterija",
            description = "Vraca sve musterije koji zadovoljavaju (opcioni) filter, sortirani po zadatom polju. Rezultati se vracaju podeljeni na stranice."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uspešna pretraga", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                    "success": true,
                    "data": {
                      "total": 1,
                      "rows": [
                        {
                          "id": 1,
                          "firstName": "Pera",
                          "lastName": "Petrovic",
                          "username": "pera123",
                          "birthDate": "2005-12-12",
                          "gender": "MALE",
                          "email": "pera@banka.com",
                          "phoneNumber": "+381641001002",
                          "address": "Knez Mihailova 6",
                          "permissions": [
                            "user.employee.view"
                          ]
                        }
                      ]
                    }
                  }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Neuspešna pretraga", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Specificirano polje za filtriranje ali nije specificirana vrednost."
                }
            """))
        )
    })
    @GetMapping("customers")
    @Authorization(permissions = { Permission.LIST_CUSTOMER })
    public ResponseEntity<?> searchCustomers(
            @Parameter(
                    description = "Redni broj stranice rezultata (podrazumevana vrednost je prva stranica)",
                    example = "0"
            )
            @RequestParam Optional<Integer> page,
            @Parameter(
                    description = "Velicina jedne stranice (podrazumevana vrednost je 10)",
                    example = "10"
            )
            @RequestParam Optional<Integer> pageSize,
            @Parameter(
                    description = "Polje po kojem se sortira. Ako nije postavljeno, sortira se po ID-u.",
                    example = "firstName"
            )
            @RequestParam Optional<String> sortField,
            @Parameter(
                    description = "Redosled sortiranja. Mora biti asc ili desc, podrazumeva se asc.",
                    example = "desc"
            )
            @RequestParam Optional<String> sortOrder,
            @Parameter(
                    description = "Polje po kojem se filtriraju rezultati. Nema filtera ako se ne postavi.",
                    example = "lastName"
            )
            @RequestParam Optional<String> filterField,
            @Parameter(
                    description = "Tekst koji se trazi u filtriranom polju. Mora se postaviti ako je postavljen filterField.",
                    example = "Petrovic"
            )
            @RequestParam Optional<String> filterValue
    ) {
        try {
            var employees = customerService.search(
                    page.orElse(0), pageSize.orElse(10),
                    sortField, sortOrder, filterField, filterValue
            );
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, employees, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
