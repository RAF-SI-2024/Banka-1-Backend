package com.banka1.user.controllers;

import com.banka1.user.DTO.response.CustomerPageResponse;
import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.DTO.response.EmployeesPageResponse;
import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import com.banka1.user.service.CustomerService;
import com.banka1.user.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

public class SearchControllerTest {
    @Mock
    private EmployeeService employeeService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    void searchEmployeesEmpty() throws Exception {
        var response = new EmployeesPageResponse(0, List.of());

        Mockito.when(employeeService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/employees"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), EmployeesPageResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(employeeService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void searchEmployees() throws Exception {
        var response = new EmployeesPageResponse(1, List.of(new EmployeeResponse(
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
                List.of())));

        Mockito.when(employeeService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/employees"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), EmployeesPageResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(employeeService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void searchEmployeesInvalid() throws Exception {
        Mockito.when(employeeService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenThrow(new RuntimeException("Poruka"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/employees"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(employeeService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void searchCustomersEmpty() throws Exception {
        var response = new CustomerPageResponse(0, List.of());

        Mockito.when(customerService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/customers"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), CustomerPageResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(customerService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void searchCustomers() throws Exception {
        var response = new CustomerPageResponse(1, List.of(new CustomerResponse(
                1L,
                "Petar",
                "Petrovic",
                "ppetrovic",
                "2000-03-03",
                Gender.MALE,
                "ppetrovic@banka.rs",
                "99999999",
                "Ulica",
                List.of())));

        Mockito.when(customerService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/customers"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), CustomerPageResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(customerService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void searchCustomersInvalid() throws Exception {
        Mockito.when(customerService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenThrow(new RuntimeException("Poruka"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/search/customers"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(customerService).search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
