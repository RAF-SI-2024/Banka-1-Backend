package com.banka1.user.controllers;

import com.banka1.user.DTO.request.CreateEmployeeRequest;
import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.DTO.request.UpdateEmployeeRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.aspect.Authorization;
import com.banka1.user.model.Employee;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.model.helper.Position;
import com.banka1.user.service.EmployeeService;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/employees")
@Tag(name = "Employee API", description = "API za upravljanje zaposlenima")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @Operation(
            summary = "Dobavljanje informacija o zaposlenom datog ID-a"
    )
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
            return ResponseTemplate.create(ResponseEntity.ok(), true, employee, null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @PostMapping("/")
    @Authorization(permissions = { Permission.CREATE_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Kreiranje zaposlenog", description = "Dodaje novog zaposlenog u sistem.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zaposleni uspešno kreiran"),
            @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju")
    })
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest createEmployeeRequest) {
        Employee savedEmployee = null;
        try {
            savedEmployee = employeeService.createEmployee(createEmployeeRequest);
        } catch (RuntimeException e) {
            log.error("createEmployee: ", e);
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", savedEmployee.getId());
        data.put("message", "Zaposleni uspešno kreiran");

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.CREATED), true, data, null);
    }

    @PutMapping("/set-password")
    @Operation(summary = "Postavljanje lozinke", description = "Postavljanje lozinke i validacija mejla nakon kreiranja musterije")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspeh"),
    })
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest setPasswordRequest) {
        System.out.println(setPasswordRequest);
        try {
            employeeService.setPassword(setPasswordRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, Map.of("message", "Lozinka uspešno postavljena"), null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @PutMapping("/{id}")
    @Authorization(permissions = { Permission.EDIT_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Ažuriranje zaposlenog", description = "Menja podatke zaposlenog na osnovu ID-a.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Podaci uspešno ažurirani"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
            @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju")
    })
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody UpdateEmployeeRequest updateEmployeeRequest) {
        try {
            employeeService.updateEmployee(id, updateEmployeeRequest);
        } catch (RuntimeException e) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), e);
        }

        return ResponseTemplate.create(ResponseEntity.ok(), true, "Podaci korisnika ažurirani", null);
    }

    @DeleteMapping("/{id}")
    @Authorization(permissions = { Permission.DELETE_EMPLOYEE }, positions = { Position.HR })
    @Operation(summary = "Brisanje zaposlenog", description = "Briše zaposlenog iz sistema po ID-u.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Korisnik uspešno obrisan"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
            @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju")
    })
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (!employeeService.existsById(id)) {
            return ResponseTemplate.create(ResponseEntity.status(HttpStatus.NOT_FOUND), false, null, "Zaposleni sa ID-em " + id + " nije pronađen.");
        }

        try {
            employeeService.deleteEmployee(id);
            return ResponseTemplate.create(ResponseEntity.ok(), true, "Korisnik uspešno obrisan", null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }

    @PutMapping("/{id}/permissions")
    @Authorization(permissions = { Permission.SET_EMPLOYEE_PERMISSION }, positions = { Position.HR })
    @Operation(summary = "Ažuriranje permisija zaposlenom", description = "Menja dozvole zaposlenog.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permisije uspešno ažurirane"),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen"),
            @ApiResponse(responseCode = "403", description = "Nemaš permisije za ovu akciju")
    })
    public ResponseEntity<?> updatePermissions(@PathVariable Long id, @RequestBody UpdatePermissionsRequest updatePermissionsRequest){

        if (!employeeService.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Zaposleni sa ID-em " + id + " nije pronađen.");
        }

        try {
            employeeService.updatePermissions(id, updatePermissionsRequest);
            return ResponseTemplate.create(ResponseEntity.ok(), true, "Permisije korisnika ažurirane", null);
        } catch (Exception e) {
            return ResponseTemplate.create(ResponseEntity.badRequest(), e);
        }
    }
}
