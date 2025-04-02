package com.banka1.user.services;

import com.banka1.user.DTO.request.CreateEmployeeRequest;
import com.banka1.user.DTO.request.UpdateEmployeeRequest;
import com.banka1.user.DTO.request.UpdatePermissionsRequest;
import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.DTO.response.EmployeesPageResponse;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.model.Employee;
import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.service.EmployeeService;
import com.banka1.user.service.SetPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AssertionErrors;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SetPasswordService setPasswordService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private MessageHelper messageHelper;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Marko");
        employee.setLastName("Markovic");
        employee.setGender(Gender.MALE);
    }

    @Test
    void testCreateEmployee() {
        CreateEmployeeRequest dto = new CreateEmployeeRequest();
        dto.setFirstName("Marko");
        dto.setLastName("Markovic");

        when(modelMapper.map(any(CreateEmployeeRequest.class), eq(Employee.class))).thenReturn(employee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        Employee result = employeeService.createEmployee(dto);

        assertNotNull(result);
        assertEquals("Marko", result.getFirstName());
        assertEquals("Markovic", result.getLastName());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    void testUpdateEmployee() {
        UpdateEmployeeRequest dto = new UpdateEmployeeRequest();
        dto.setFirstName("Marko");
        dto.setLastName("Markovic");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);


        Employee result = employeeService.updateEmployee(1L, dto);

        assertNotNull(result);
        assertEquals("Marko", result.getFirstName());
        assertEquals("Markovic", result.getLastName());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }


    @Test
    void testUpdatePermissions() {
        UpdatePermissionsRequest dto = new UpdatePermissionsRequest();
        dto.setPermissions(List.of(Permission.READ_EMPLOYEE, Permission.CREATE_EMPLOYEE));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);


        Employee result = employeeService.updatePermissions(1L, dto);

        assertNotNull(result);

        String permissionsAsString = result.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        assertEquals("READ_EMPLOYEE, CREATE_EMPLOYEE", permissionsAsString);

        verify(employeeRepository, times(1)).save(any(Employee.class));
    }


    @Test
    void testDeleteEmployee(){
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test@example.com");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        employee.setEmail("other@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(1L);

        // Provera da li je metoda delete pozvana tačno jednom
        verify(employeeRepository, times(1)).delete(employee);
    }

    @Test
    void testUpdateEmployee_NotFound() {
        UpdateEmployeeRequest dto = new UpdateEmployeeRequest();
        dto.setFirstName("Novi");
        dto.setLastName("Naziv");

        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> employeeService.updateEmployee(1L, dto));
    }

    @Test
    void findByIdSuccess() {
        String id = "1";
        var entity = getEmployee();

        Mockito.when(employeeRepository.findById(1L)).thenReturn(Optional.of(entity));

        var response = getEmployeeResponse();

        AssertionErrors.assertEquals("Response", response, employeeService.findById(id));

        Mockito.verify(employeeRepository).findById(1L);
    }

    private static EmployeeResponse getEmployeeResponse() {
        return new EmployeeResponse(
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
    }

    private static Employee getEmployee() {
        var entity = new Employee();

        entity.setFirstName("Petar");
        entity.setLastName("Petrovic");
        entity.setUsername("ppetrovic");
        entity.setBirthDate("9999-09-09");
        entity.setGender(Gender.MALE);
        entity.setEmail("ppetrovic@banka.rs");
        entity.setPhoneNumber("99999999");
        entity.setAddress("Ulica");
        entity.setPosition(Position.WORKER);
        entity.setDepartment(Department.IT);
        entity.setActive(true);
        entity.setIsAdmin(false);
        entity.setPermissions(List.of());
        entity.setId(1L);
        return entity;
    }

    @Test
    void findByIdNotFound() {
        String id = "1";

        Mockito.when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        AssertionErrors.assertNull("Response", employeeService.findById(id));

        Mockito.verify(employeeRepository).findById(1L);
    }

    @Test
    void findByIdInvalidId() {
        String id = "Petar";

        try {
            employeeService.findById(id);
            fail("No exception.");
        } catch (Exception e) {
            AssertionErrors.assertNotNull("Error", e);
        }
    }

    @Test
    void search() {
        var response = new EmployeesPageResponse(1, List.of(getEmployeeResponse()));

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Mockito.when(employeeRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(getEmployee()), pageable, 1));

        AssertionErrors.assertEquals("Response", response,
                employeeService.search(0, 10, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        Mockito.verify(employeeRepository).findAll(pageable);
    }

    @Test
    void searchNoFilterValue() {
        try {
            employeeService.search(0, 10, Optional.empty(), Optional.empty(), Optional.of("firstName"), Optional.empty());
            fail("No exception.");
        } catch (Exception e) {
            AssertionErrors.assertNotNull("Error", e);
        }
    }

    @Test
    void testCreateEmployee_MissingFields() {
        CreateEmployeeRequest dto = new CreateEmployeeRequest();

        when(modelMapper.map(any(CreateEmployeeRequest.class), eq(Employee.class))).thenReturn(new Employee());

        assertThrows(RuntimeException.class, () -> employeeService.createEmployee(dto));
    }

    @Test
    void testUpdatePermissions_EmployeeNotFound() {
        UpdatePermissionsRequest dto = new UpdatePermissionsRequest();
        dto.setPermissions(List.of(Permission.READ_EMPLOYEE));

        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> employeeService.updatePermissions(1L, dto));
    }

    @Test
    void testDeleteEmployee_EmployeeNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> employeeService.deleteEmployee(1L));
    }



    @Test
    void testSearchInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> employeeService.search(0, 0, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void testFindById_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> employeeService.findById(null));
    }

    @Test
    void testFetchActuaries() throws Exception {

            Employee employee1 = new Employee();
            employee1.setId(1L);
            employee1.setFirstName("John");
            employee1.setLastName("Doe");
            employee1.setUsername("jdoe");
            employee1.setBirthDate("2000-01-01");
            employee1.setGender(Gender.MALE);
            employee1.setEmail("jdoe@banka.rs");
            employee1.setPhoneNumber("123456");
            employee1.setAddress("Street 1");
            employee1.setPosition(Position.WORKER);
            employee1.setDepartment(Department.IT);
            employee1.setActive(true);
            employee1.setIsAdmin(false);
            employee1.setPermissions(List.of());

            Employee employee2 = new Employee();
            employee2.setId(2L);
            employee2.setFirstName("Jane");
            employee2.setLastName("Smith");
            employee2.setUsername("jsmith");
            employee2.setBirthDate("2001-02-02");
            employee2.setGender(Gender.FEMALE);
            employee2.setEmail("jsmith@banka.rs");
            employee2.setPhoneNumber("654321");
            employee2.setAddress("Street 2");
            employee2.setPosition(Position.WORKER);
            employee2.setDepartment(Department.IT);
            employee2.setActive(true);
            employee2.setIsAdmin(false);
            employee2.setPermissions(List.of());

            List<Employee> actuaryList = Arrays.asList(employee1, employee2);

            EmployeeResponse response1 = new EmployeeResponse(
                    1L, "John", "Doe", "jdoe", "2000-01-01",
                    Gender.MALE, "jdoe@banka.rs", "123456", "Street 1",
                    Position.WORKER, Department.IT, true, false, List.of()
            );

            EmployeeResponse response2 = new EmployeeResponse(
                    2L, "Jane", "Smith", "jsmith", "2001-02-02",
                    Gender.FEMALE, "jsmith@banka.rs", "654321", "Street 2",
                    Position.WORKER, Department.IT, true, false, List.of()
            );

            List<EmployeeResponse> responseList = Arrays.asList(response1, response2);


            when(employeeRepository.getActuaries()).thenReturn(actuaryList);
            assertThat(employeeService.getAllActuaries()).contains(response1, response2);
            Mockito.verify(employeeRepository).getActuaries();

    }

}
