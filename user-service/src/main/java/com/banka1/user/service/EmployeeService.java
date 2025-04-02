package com.banka1.user.service;

import com.banka1.common.model.Department;
import com.banka1.user.DTO.request.*;
import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.DTO.response.EmployeesPageResponse;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.model.Employee;
import com.banka1.common.model.Position;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final SetPasswordService setPasswordService;
    private final EmployeeRepository employeeRepository;
    private final JmsTemplate jmsTemplate;
    private final ModelMapper modelMapper;
    private final MessageHelper messageHelper;
    private final Random random = new Random();

    @Value("${destination.email}")
    private String destinationEmail;
    @Value("${frontend.url}")
    private String frontendUrl;

    public EmployeeResponse findById(String id) {
        return findById(Long.parseLong(id));
    }

    public EmployeeResponse findById(long id) {
        var employeeOptional = employeeRepository.findById(id);
        if (employeeOptional.isEmpty())
            return null;
        var employee = employeeOptional.get();
        return getEmployeeResponse(employee);
    }

    public EmployeeResponse findInLegal() {
        var employees = employeeRepository.findByDepartment(Department.LEGAL);
        if (employees.isEmpty())
            return null;
        return getEmployeeResponse(employees.get(random.nextInt(employees.size())));
    }


    public Employee createEmployee(CreateEmployeeRequest createEmployeeRequest) {
        // Provera da li već postoji nalog sa istim email-om
        if (employeeRepository.existsByEmail(createEmployeeRequest.getEmail())) {
            throw new RuntimeException("Nalog sa ovim email-om već postoji!");
        }
        Employee employee = modelMapper.map(createEmployeeRequest, Employee.class);
        employee.setActive(createEmployeeRequest.getActive());

        String verificationCode = UUID.randomUUID().toString();
        employee.setVerificationCode(verificationCode);

        if (employee.getGender() == null) {
            throw new RuntimeException("Polje pol je obavezno");
        }

        NotificationRequest emailDTO = new NotificationRequest();
        emailDTO.setSubject("Nalog uspešno kreiran");
        emailDTO.setEmail(employee.getEmail());
        emailDTO
                .setMessage("Vaš nalog je uspešno kreiran. Kliknite na sledeći link da biste postavili lozinku: "
                        + frontendUrl + "/set-password?token=" + verificationCode);
        emailDTO.setFirstName(employee.getFirstName());
        emailDTO.setLastName(employee.getLastName());
        emailDTO.setType("email");

        // Saving the customer in the database gives it an ID, which can be used to generate the set-password token
        employee = employeeRepository.save(employee);

        setPasswordService.saveSetPasswordRequest(verificationCode, employee.getId(), false);

        jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));

        return employee;
    }

    public Employee updateEmployee(Long id, UpdateEmployeeRequest updateEmployeeRequest) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zaposleni nije pronađen"));

        // Ažuriranje dozvoljenih polja
        Optional.ofNullable(updateEmployeeRequest.getFirstName()).ifPresent(existingEmployee::setFirstName);
        Optional.ofNullable(updateEmployeeRequest.getLastName()).ifPresent(existingEmployee::setLastName);
        Optional.ofNullable(updateEmployeeRequest.getPhoneNumber()).ifPresent(existingEmployee::setPhoneNumber);
        Optional.ofNullable(updateEmployeeRequest.getAddress()).ifPresent(existingEmployee::setAddress);
        Optional.ofNullable(updateEmployeeRequest.getPosition()).ifPresent(existingEmployee::setPosition);
        Optional.ofNullable(updateEmployeeRequest.getDepartment()).ifPresent(existingEmployee::setDepartment);
        Optional.ofNullable(updateEmployeeRequest.getActive()).ifPresent(existingEmployee::setActive);
        Optional.ofNullable(updateEmployeeRequest.getIsAdmin()).ifPresent(existingEmployee::setIsAdmin);
        Optional.ofNullable(updateEmployeeRequest.getGender()).ifPresent(existingEmployee::setGender);
        Optional.ofNullable(updateEmployeeRequest.getUsername()).ifPresent(existingEmployee::setUsername);
        Optional.ofNullable(updateEmployeeRequest.getEmail()).ifPresent(existingEmployee::setEmail);
        Optional.ofNullable(updateEmployeeRequest.getBirthDate()).ifPresent(existingEmployee::setBirthDate);

        return employeeRepository.save(existingEmployee);
    }

    public Employee updatePermissions(Long id, UpdatePermissionsRequest updatePermissionsRequest){
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zaposleni nije pronađen"));


        Optional.ofNullable(updatePermissionsRequest.getPermissions()).ifPresent(existingEmployee::setPermissions);

        return employeeRepository.save(existingEmployee);
    }
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zaposleni nije pronađen"));

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ne možeš obrisati samog sebe
        if (employee.getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("Ne možete obrisati sami sebe");
        }

        employeeRepository.delete(employee);
    }

    public boolean existsById(Long id) {
        return employeeRepository.existsById(id);
    }

    private static EmployeeResponse getEmployeeResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getUsername(),
                employee.getBirthDate(),
                employee.getGender(),
                employee.getEmail(),
                employee.getPhoneNumber(),
                employee.getAddress(),
                employee.getPosition(),
                employee.getDepartment(),
                employee.getActive(),
                employee.getIsAdmin(),
                employee.getPermissions());
    }

    public EmployeesPageResponse search(int page, int pageSize, Optional<String> sortField, Optional<String> sortOrder, Optional<String> filterField, Optional<String> filterValueOptional) {
        var direction = Sort.Direction.ASC;
        if (sortOrder.isPresent()) {
            switch (sortOrder.get().toLowerCase()) {
                case "asc" -> {}
                case "desc" -> direction = Sort.Direction.DESC;
                default -> throw new RuntimeException("Smer sortiranja nije prepoznat.");
            }
        }

        var pageRequest = PageRequest.of(page, pageSize, Sort.by(direction, sortField.orElse("id")));
        Page<Employee> employeePage;

        if (filterField.isPresent()) {
            var matcher = ExampleMatcher.matching().withMatcher(filterField.get(), contains());
            if (filterValueOptional.isEmpty())
                throw new RuntimeException("Specificirano polje za filtriranje ali nije specificirana vrednost.");
            var filterValue = filterValueOptional.get();
            var employee = new Employee();
            switch (filterField.get()) {
                case "id" -> employee.setId(Long.valueOf(filterValue));
                case "firstName" -> employee.setFirstName(filterValue);
                case "lastName" -> employee.setLastName(filterValue);
                case "birthDate" -> employee.setBirthDate(filterValue);
                case "gender" -> employee.setGender(Gender.valueOf(filterValue.toUpperCase()));
                case "email" -> employee.setEmail(filterValue);
                case "phoneNumber" -> employee.setPhoneNumber(filterValue);
                case "address" -> employee.setAddress(filterValue);
                case "position" -> employee.setPosition(Position.valueOf(filterValue.toUpperCase()));
                case "department" -> employee.setDepartment(Department.valueOf(filterValue.toUpperCase()));
                case "active" -> employee.setActive(Boolean.valueOf(filterValue.toLowerCase()));
                case "isAdmin" -> employee.setIsAdmin(Boolean.valueOf(filterValue.toLowerCase()));
                default -> throw new RuntimeException("Polje za filtriranje nije prepoznato.");
            }
            var example = Example.of(employee, matcher);
            employeePage = employeeRepository.findAll(example, pageRequest);
        }
        else
            employeePage = employeeRepository.findAll(pageRequest);
        return new EmployeesPageResponse(
                employeePage.getTotalElements(),
                employeePage.stream().map(EmployeeService::getEmployeeResponse).toList()
        );
    }

    public List<EmployeeResponse> getAllActuaries(){
        return employeeRepository.getActuaries()
                        .stream()
                        .map(EmployeeService::getEmployeeResponse)
                        .toList();

    }
}
