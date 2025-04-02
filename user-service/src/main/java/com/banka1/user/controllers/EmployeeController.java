package com.banka1.user.controllers;

import com.banka1.user.DTO.request.CreateEmployeeRequest;
import com.banka1.user.DTO.request.UpdateEmployeeRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.model.Employee;
import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/employees")
@Tag(name = "Employee API", description = "API za upravljanje zaposlenima")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(
        summary = "Dobavljanje informacija o zaposlenom datog ID-a"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Uspešno dobijene informacije o zaposlenom", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "id": 2,
                     "firstName": "Pera",
                     "lastName": "Petrovic",
                     "username": "pera123",
                     "birthDate": "1990-07-07",
                     "gender": "MALE",
                     "email": "pera@banka.com",
                     "phoneNumber": "+381641001000",
                     "address": "Knez Mihailova 6",
                     "position": "MANAGER",
                     "department": "IT",
                     "active": true,
                     "isAdmin": false,
                     "permissions": [
                       "user.customer.create"
                     ]
                   }
                 }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Neispravni podaci ili korisnik ne postoji", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik nije pronadjen."
                }
            """))
        )
    })
    @GetMapping("/{id}")
    @Authorization(permissions = { Permission.READ_EMPLOYEE }, allowIdFallback = true )
    public ResponseEntity<?> getById(
            @Parameter(required = true, example = "1")
            @PathVariable String id
    ) {
        try {
            var employee = employeeService.findById(id);
            if (employee == null)
                return ResponseTemplate.create(ResponseEntity.status(HttpStatusCode.valueOf(404)),
                        false, null, "Korisnik nije pronadjen.");
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, employee, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST),false, null, e.getMessage());
        }
    }

    @PostMapping("/")
    @Authorization(permissions = { Permission.CREATE_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Kreiranje zaposlenog", description = "Dodaje novog zaposlenog u sistem.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Zaposleni uspešno kreiran", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                   "success": true,
                   "data": {
                     "id": 6,
                     "message": "Zaposleni uspešno kreiran"
                   }
                 }
            """))
        ),
        @ApiResponse(responseCode = "400", description = "Došlo je do greške prilikom kreiranja zaposlenog", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "error": "Došlo je do greške prilikom kreiranja zaposlenog.",
                  "success": false
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
        )
    })
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest createEmployeeRequest) {
        Employee savedEmployee;
        try {
            savedEmployee = employeeService.createEmployee(createEmployeeRequest);
        } catch (RuntimeException e) {
            log.error("createEmployee: ", e);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", savedEmployee.getId());
        data.put("message", "Zaposleni uspešno kreiran");

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, data, null);
    }


    @PutMapping("/{id}")
    @Authorization(permissions = { Permission.EDIT_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Ažuriranje zaposlenog", description = "Menja podatke zaposlenog na osnovu ID-a.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Podaci uspešno ažurirani", content = @Content(mediaType = "application/json",
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
                  "error": "Nedovoljna autorizacija.",
                  "success": false
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Zaposleni nije pronađen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Zaposleni nije pronađen"
                }
            """)))
    })
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody UpdateEmployeeRequest updateEmployeeRequest) {
        try {
            employeeService.updateEmployee(id, updateEmployeeRequest);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND),false,null, "Zaposleni sa ID-em " + id + " nije pronađen.");
        }

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, "Podaci korisnika ažurirani", null);
    }

    @DeleteMapping("/{id}")
    @Authorization(permissions = { Permission.DELETE_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Brisanje zaposlenog", description = "Briše zaposlenog iz sistema po ID-u.")
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
                  "error": "Nedovoljna autorizacija.",
                  "success": false
                }
            """))
        ),
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronadjen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Korisnik nije pronađen"
                }
            """)))
    })
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (!employeeService.existsById(id)) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Zaposleni sa ID-em " + id + " nije pronađen.");
        }

        try {
            employeeService.deleteEmployee(id);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, "Korisnik uspešno obrisan", null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @PutMapping("/{id}/permissions")
    @Authorization(permissions = { Permission.SET_EMPLOYEE_PERMISSION }, positions = { Position.HR })
    @Operation(summary = "Ažuriranje permisija zaposlenom", description = "Menja dozvole zaposlenog.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permisije uspešno ažurirane", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "message": "Permisije korisnika ažurirane"
                  }
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
        @ApiResponse(responseCode = "404", description = "Korisnik nije pronadjen", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "error": "Zaposleni nije pronađen."
                }
            """)))
    })
    public ResponseEntity<?> updatePermissions(@PathVariable Long id, @RequestBody UpdatePermissionsRequest updatePermissionsRequest){

        if (!employeeService.existsById(id)) {

            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Zaposleni sa ID-em " + id + " nije pronađen.");
        }

        try {
            employeeService.updatePermissions(id, updatePermissionsRequest);
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, "Permisije korisnika ažurirane", null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.BAD_REQUEST), false, null, e.getMessage());
        }
    }

    @GetMapping("/actuaries")
    public ResponseEntity<?> fetchActuaries(){
      var employees = employeeService.getAllActuaries();
      return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, employees, null);
    }


}
