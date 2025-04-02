package com.banka1.user.controllers;

import com.banka1.common.model.Department;
import com.banka1.user.aspect.Authorization;
import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import com.banka1.user.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/metadata")
@Tag(name = "User Metadata API", description = "API za fetchovanje metapodataka u vezi korisnika")
public class MetadataController {
    @Authorization(permissions = { Permission.CREATE_EMPLOYEE, Permission.EDIT_EMPLOYEE })
    @GetMapping("/departments")
    @Operation(summary = "Dobavljanje odeljenja", description = "Vraća listu stringova koji korespondiraju vrednostima za sva odeljenja u banci.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "list": [
                      "WORKER",
                      "HR",
                      "..."
                    ]
                  }
                }
            """))
        )
    })
    public ResponseEntity<?> getAllDepartments() {
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("list", Arrays.stream(Department.values()).map(Department::toString).collect(Collectors.toList())), null);
    }

    @Authorization(permissions = { Permission.CREATE_EMPLOYEE, Permission.EDIT_EMPLOYEE })
    @GetMapping("/permissions")
    @Operation(summary = "Dobavljanje permisija", description = "Vraća listu stringova koji korespondiraju vrednostima za sve permisije u sistemu.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "list": [
                      "user.employee.create",
                      "user.employee.view",
                      "..."
                    ]
                  }
                }
            """))
        )
    })
    public ResponseEntity<?> getAllPermissions() {
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("list", Arrays.stream(Permission.values()).map(Permission::toString).collect(Collectors.toList())), null);
    }

    @Authorization(permissions = { Permission.CREATE_EMPLOYEE, Permission.EDIT_EMPLOYEE })
    @GetMapping("/positions")
    @Operation(summary = "Dobavljanje pozicija", description = "Vraća listu stringova koji korespondiraju vrednostima za sve pozicije u banci.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "data": {
                    "list": [
                      "IT",
                      "HR",
                      "..."
                    ]
                  }
                }
            """))
        )
    })
    public ResponseEntity<?> getAllPositions() {
        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, Map.of("list", Arrays.stream(Position.values()).map(Position::toString).collect(Collectors.toList())), null);
    }
}
