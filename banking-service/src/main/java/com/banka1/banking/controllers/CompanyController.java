package com.banka1.banking.controllers;

import com.banka1.banking.aspect.CompanyAuthorization;
import com.banka1.banking.dto.CreateCompanyDTO;

import com.banka1.banking.models.Company;
import com.banka1.banking.services.CompanyService;

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
@RequestMapping("/companies")
@Tag(name = "Company API", description = "API za upravljanje kompanijama")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/")
    @Operation(summary = "Kreiranje kompanije", description = "Kreira novu kompaniju.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kompanija uspešno kreirana.", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "id": 1,
                    "message": "Kompanija uspešno kreirana."
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
            @ApiResponse(responseCode = "400", description = "Nevalidni podaci.", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nevalidni podaci."
                }
            """))
            )
    })
    @CompanyAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> createCompany(@RequestBody CreateCompanyDTO createCompanyDTO) {
        try {
            Company company = companyService.createCompany(createCompanyDTO);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, Map.of("id", company.getId(), "message", ResponseMessage.COMPANY_CREATED.toString()), null);
        } catch (RuntimeException e) {
            log.error("Greška prilikom kreiranja kompanije: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/")
    @Operation(summary = "Pregled svih kompanija", description = "Izlistavanje svih kompanija koje imamo nu sistemu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sve kompanije", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                   "data": {
                    "companies": [
                        {
                            "id": 1,
                            "name": "Ime firme raa",
                            "address": "Bulevar Banka 1",
                            "vatNumber": "111111111",
                            "companyNumber": "11111111"
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
            @ApiResponse(responseCode = "404", description = "nema kompanija u sistemu", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "prazan sistem"
                }
            """))
            )
    })
    @CompanyAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getCompanies() {
        try {
            List<Company> companies  =  companyService.getCompanies();
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("companies", companies), null);
        } catch (Exception e) {
            log.error("Greška prilikom trazenja kompanija: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/{company_id}")
    @Operation(summary = "Pregled detalja kompanije", description = "Detalji o kompaniji kojima pristupa zaposleni")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalji kompanije.", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                   "data": {
                    "company": {
                        "id": 1,
                        "name": "Ime firme raa",
                        "address": "Bulevar Banka 1",
                        "vatNumber": "111111111",
                        "companyNumber": "11111111"
                    }
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
            @ApiResponse(responseCode = "404", description = "nema ove kompanije", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nema kompanije sa datim id"
                }
            """))
            )
    })
    @CompanyAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getCompanyByID(@PathVariable("company_id") Long companyId) {
        try {
            Company company  =  companyService.getCompany(companyId);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("company", company), null);
        } catch (Exception e) {
            log.error("Greška prilikom trazenja kompanije: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/basList")
    @Operation(summary = "Lista svih sifara delatnosti", description = "sifr4e delatnosti i njihovi opisi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "sifre delatnosti", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                   "data": {
                    "basList": [
                        "6201 -> Computer programming",
                        "4711 -> Retail sale in non-specialized stores with food, beverages, or tobacco predominating",
                        "6820 -> Renting and operating of own or leased real estate",
                        "5610 -> Restaurants and mobile food service activities",
                        "4932 -> Taxi operation"
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
            @ApiResponse(responseCode = "404", description = "nema", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "nema informacija o siframa delatnosti"
                }
            """))
            )
    })
    @CompanyAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getBasList() {
        try {
            List<String> lista =  companyService.getBusinessActivityCodes();
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("basList", lista), null);
        } catch (Exception e) {
            log.error("Greška prilikom trazenja basova: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/{owner_id}")
    @Operation(summary = "Pregled svih kompanija za vlasnika", description = "Izlistavanje svih kompanija istog vlasnika")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista kompanija korisnika.", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                   "data": {
                      "companies": [
                        {
                            "id": 1,
                            "name": "Ime firme raa",
                            "address": "Bulevar Banka 1",
                            "vatNumber": "111111111",
                            "companyNumber": "11111111"
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
            @ApiResponse(responseCode = "404", description = "Nema kaompanija ovog kornsnika.", content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Nema kompanija ovog korisnika."
                }
            """))
            )
    })
    @CompanyAuthorization(employeeOnlyOperation = true)
    public ResponseEntity<?> getCompaniesByOwnerID(@PathVariable("owner_id") Long ownerId) {
        try {
            List<Company> companies = companyService.findAllByOwnerId(ownerId);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("companies", companies), null);
        } catch (Exception e) {
            log.error("Greška prilikom trazenja kartica: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }
}
