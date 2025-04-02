package com.banka1.user.controllers;

import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import com.banka1.user.DTO.request.CreateEmployeeRequest;
import com.banka1.user.DTO.request.UpdateEmployeeRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.model.Employee;
import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
    }

    @Test
    void testCreateEmployee() throws Exception {
        CreateEmployeeRequest dto = new CreateEmployeeRequest();
        dto.setFirstName("Marko");
        dto.setLastName("Markovic");

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Marko");
        employee.setLastName("Markovic");

        when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(employee);

        mockMvc.perform(post("/api/users/employees/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void testUpdateEmployee() throws Exception {
        UpdateEmployeeRequest dto = new UpdateEmployeeRequest();
        dto.setFirstName("Petar");
        dto.setLastName("Petrovic");

        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setFirstName("Petar");
        updatedEmployee.setLastName("Petrovic");

        when(employeeService.updateEmployee(Mockito.eq(1L), any(UpdateEmployeeRequest.class))).thenReturn(updatedEmployee);

        mockMvc.perform(put("/api/users/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Podaci korisnika ažurirani"));
    }

    @Test
    void testDeleteEmployee() throws Exception {
        when(employeeService.existsById(1L)).thenReturn(true);
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/users/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Korisnik uspešno obrisan"));
    }

    @Test
    void testUpdatePermissions() throws Exception {
        UpdatePermissionsRequest dto = new UpdatePermissionsRequest();
        dto.setPermissions(List.of(Permission.READ_EMPLOYEE));

        Employee updatedEmployee = new Employee();
        updatedEmployee.setId(1L);
        updatedEmployee.setPermissions(List.of(Permission.CREATE_EMPLOYEE));

        when(employeeService.existsById(1L)).thenReturn(true);
        when(employeeService.updatePermissions(Mockito.eq(1L), any(UpdatePermissionsRequest.class))).thenReturn(updatedEmployee);

        mockMvc.perform(put("/api/users/employees/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Permisije korisnika ažurirane"));
    }

    @Test
    void findByIdSuccess() throws Exception {
        String id = "1";
        var response = new EmployeeResponse(
                1L,
                "Petar",
                "Petrovic",
                "ppetrovic",
                "9999-09-09",
                Gender.MALE,
                "ppetrovic@banka.rs",
                "99999999",
                "Ulica",
                Position.WORKER,
                Department.IT,
                true,
                false,
                List.of());

        Mockito.when(employeeService.findById(id)).thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/employees/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(id))
                .andReturn().getResponse().getContentAsString();
        System.out.println(responseContent);
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), EmployeeResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(employeeService).findById(id);
    }

    @Test
    void findByIdNotFound() throws Exception {
        String id = "1";

        Mockito.when(employeeService.findById(id)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/employees/" + id))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(employeeService).findById(id);
    }

    @Test
    void findByIdBadRequest() throws Exception {
        String id = "1";

        Mockito.when(employeeService.findById(id)).thenThrow(new RuntimeException("Poruka"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/employees/" + id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(employeeService).findById(id);
    }

    @Test
    void fetchActuaries() throws Exception {
        Employee actuary1 = new Employee();
        actuary1.setId(1L);
        actuary1.setFirstName("John");
        actuary1.setLastName("Doe");

        Employee actuary2 = new Employee();
        actuary2.setId(2L);
        actuary2.setFirstName("Jane");
        actuary2.setLastName("Smith");

        List<Employee> actuaryList = Arrays.asList(actuary1, actuary2);

        doReturn(actuaryList).when(employeeService).getAllActuaries();

        mockMvc.perform(get("/api/users/employees/actuaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].firstName").value("John"))
                .andExpect(jsonPath("$.data[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].firstName").value("Jane"))
                .andExpect(jsonPath("$.data[1].lastName").value("Smith"));

        Mockito.verify(employeeService).getAllActuaries();
    }
}
