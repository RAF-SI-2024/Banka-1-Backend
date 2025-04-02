package com.banka1.user.controllers;


import com.banka1.common.model.Permission;
import com.banka1.user.DTO.banking.CreateAccountWithoutOwnerIdDTO;
import com.banka1.user.DTO.banking.helper.AccountStatus;
import com.banka1.user.DTO.banking.helper.AccountSubtype;
import com.banka1.user.DTO.banking.helper.AccountType;
import com.banka1.user.DTO.banking.helper.CurrencyType;
import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.request.UpdateCustomerRequest;
import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.mapper.CustomerMapper;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.service.CustomerService;
import com.banka1.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {

    private MockMvc mockMvc;
    @Mock
    private AuthService authService;

    @Mock
    private Claims claims;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();
    }

    @Test
    void testCreateCustomer() throws Exception {
        var createAccountDTO = new CreateAccountWithoutOwnerIdDTO();
        createAccountDTO.setCurrency(CurrencyType.RSD);
        createAccountDTO.setType(AccountType.CURRENT);
        createAccountDTO.setSubtype(AccountSubtype.PERSONAL);
        createAccountDTO.setDailyLimit(0.0);
        createAccountDTO.setMonthlyLimit(0.0);
        createAccountDTO.setStatus(AccountStatus.ACTIVE);
        createAccountDTO.setCreateCard(true);
        var customerDTO = new CreateCustomerRequest(
                "Petar",
                "Petrovic",
                "ppetrovic",
                "2000-03-03",
                Gender.MALE,
                "ppetrovic@banka.rs",
                "99999999",
                "Ulica",
                createAccountDTO);

        Customer customer = CustomerMapper.dtoToCustomer(customerDTO);
        customer.setId(1L);
        var claims = Jwts.claims().add("id", 1L).build();

        when(customerService.createCustomer(any(CreateCustomerRequest.class), anyLong())).thenReturn(customer);

        when(authService.parseToken(any())).thenReturn(claims);

        mockMvc.perform(post("/api/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdateCustomer() throws Exception {
        var customerDTO = new UpdateCustomerRequest();
        customerDTO.setFirstName("Petar");
        customerDTO.setLastName("Petrovic");

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("Petar");
        customer.setLastName("Petrovic");

        when(customerService.updateCustomer(eq(1L), any(UpdateCustomerRequest.class))).thenReturn(Optional.of(customer));

        mockMvc.perform(put("/api/customer/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Podaci korisnika ažurirani"));
    }

    @Test
    void testDeleteCustomer() throws Exception {
        when(customerService.deleteCustomer(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Korisnik uspešno obrisan"));
    }

//    @Test
//    void testDeleteCustomerNotFound() throws Exception {
//        when(customerService.deleteCustomer(1L)).thenReturn(false);
//
//        mockMvc.perform(delete("/api/customer/1")
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.data").value("Korisnik nije pronađen"));
//
//        verify(customerService).deleteCustomer(1L);
//    }

    @Test
    void testUpdatePermissions() throws Exception {
        List<Permission> permissions = List.of(Permission.READ_EMPLOYEE);

        Customer customer = new Customer();
        customer.setId(1L);

        when(customerService.updateCustomerPermissions(eq(1L), anyList())).thenReturn(Optional.of(customer));

        mockMvc.perform(put("/api/customer/1/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("permissions", permissions))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("Permisije ažurirane"));
    }

    @Test
    void testUpdateCustomerPermissionsNotFound() throws Exception {
        List<Permission> permissions = List.of(Permission.READ_EMPLOYEE);

        when(customerService.updateCustomerPermissions(eq(1L), anyList())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/customer/" + 1L + "/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("permissions", permissions))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Korisnik nije pronađen"));
    }

    @Test
    void findByIdSuccess() throws Exception {
        String id = "1";
        var response = new CustomerResponse(
                1L,
                "Petar",
                "Petrovic",
                "ppetrovic",
                "2000-03-03",
                Gender.MALE,
                "ppetrovic@banka.rs",
                "99999999",
                "Ulica",
                List.of());

        Mockito.when(customerService.findById(id)).thenReturn(response);

        var responseContent = mockMvc.perform(MockMvcRequestBuilders.get("/api/customer/" + id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(id))
                .andReturn().getResponse().getContentAsString();
        var responseJson = objectMapper.convertValue(objectMapper.readTree(responseContent).get("data"), CustomerResponse.class);

        AssertionErrors.assertEquals("Response", response, responseJson);

        Mockito.verify(customerService).findById(id);
    }

    @Test
    void findByIdNotFound() throws Exception {
        String id = "1";

        Mockito.when(customerService.findById(id)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/customer/" + id))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(customerService).findById(id);
    }

    @Test
    void findByIdBadRequest() throws Exception {
        String id = "1";

        Mockito.when(customerService.findById(id)).thenThrow(new RuntimeException("Poruka"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/customer/" + id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));

        Mockito.verify(customerService).findById(id);
    }
}
