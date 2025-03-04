package com.banka1.user.service;

import com.banka1.user.DTO.NotificationDTO;
import com.banka1.user.DTO.request.CreateEmployeeDto;
import com.banka1.user.DTO.request.SetPasswordDTO;
import com.banka1.user.DTO.request.UpdateEmployeeDto;
import com.banka1.user.DTO.request.UpdatePermissionsDto;
import com.banka1.user.DTO.response.EmployeeResponse;
import com.banka1.user.DTO.response.EmployeesPageResponse;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.model.Employee;
import com.banka1.user.model.helper.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final String destinationEmail;
	@Value("${frontend.url}")
	private String frontendUrl;

    @Autowired
    public EmployeeService(EmployeeRepository customerRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, @Value("${destination.email}") String destinationEmail) {
        this.employeeRepository = customerRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.destinationEmail = destinationEmail;

    }

    public EmployeeResponse findById(String id) {
        var employeeOptional = employeeRepository.findById(Long.parseLong(id));
        if (employeeOptional.isEmpty())
            return null;
        var employee = employeeOptional.get();
        return getEmployeeResponse(employee);
    }

    @Autowired
    private ModelMapper modelMapper;

    public Employee createEmployee(CreateEmployeeDto createEmployeeDto) {
        // Provera da li već postoji nalog sa istim email-om
        if (employeeRepository.existsByEmail(createEmployeeDto.getEmail())) {
            throw new RuntimeException("Nalog sa ovim email-om već postoji!");
        }
        Employee employee = modelMapper.map(createEmployeeDto, Employee.class);
        employee.setActive(createEmployeeDto.getActive());

        String verificationCode = UUID.randomUUID().toString();
        employee.setVerificationCode(verificationCode);

        if (employee.getGender() == null) {
            throw new RuntimeException("Polje pol je obavezno");
        }

        NotificationDTO emailDTO = new NotificationDTO();
        emailDTO.setSubject("Nalog uspešno kreiran");
        emailDTO.setEmail(employee.getEmail());
	    emailDTO
	    .setMessage("Vaš nalog je uspešno kreiran. Kliknite na sledeći link da biste postavili lozinku: "
			    + frontendUrl + "/set-password?token=" + verificationCode);
        emailDTO.setFirstName(employee.getFirstName());
        emailDTO.setLastName(employee.getLastName());
        emailDTO.setType("email");

        jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));

        return employeeRepository.save(employee);
    }

    public void setPassword(SetPasswordDTO setPasswordDTO) {
        Employee employee = employeeRepository.findByVerificationCode(setPasswordDTO.getCode())
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

        var salt = generateSalt();
        var hashed = passwordEncoder.encode(setPasswordDTO.getPassword() + salt);
        employee.setPassword(hashed);
        employee.setSaltPassword(salt);
        employee.setVerificationCode(null);

        employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, UpdateEmployeeDto updateEmployeeDto) {
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zaposleni nije pronađen"));

        // Ažuriranje dozvoljenih polja
        Optional.ofNullable(updateEmployeeDto.getFirstName()).ifPresent(existingEmployee::setFirstName);
        Optional.ofNullable(updateEmployeeDto.getLastName()).ifPresent(existingEmployee::setLastName);
        Optional.ofNullable(updateEmployeeDto.getPhoneNumber()).ifPresent(existingEmployee::setPhoneNumber);
        Optional.ofNullable(updateEmployeeDto.getAddress()).ifPresent(existingEmployee::setAddress);
        Optional.ofNullable(updateEmployeeDto.getPosition()).ifPresent(existingEmployee::setPosition);
        Optional.ofNullable(updateEmployeeDto.getDepartment()).ifPresent(existingEmployee::setDepartment);
        Optional.ofNullable(updateEmployeeDto.getActive()).ifPresent(existingEmployee::setActive);
        Optional.ofNullable(updateEmployeeDto.getIsAdmin()).ifPresent(existingEmployee::setIsAdmin);
        Optional.ofNullable(updateEmployeeDto.getGender()).ifPresent(existingEmployee::setGender);
        Optional.ofNullable(updateEmployeeDto.getUsername()).ifPresent(existingEmployee::setUsername);
        Optional.ofNullable(updateEmployeeDto.getEmail()).ifPresent(existingEmployee::setEmail);
        Optional.ofNullable(updateEmployeeDto.getBirthDate()).ifPresent(existingEmployee::setBirthDate);

        return employeeRepository.save(existingEmployee);
    }

    public Employee updatePermissions(Long id, UpdatePermissionsDto updatePermissionsDto){
        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zaposleni nije pronađen"));


        Optional.ofNullable(updatePermissionsDto.getPermissions()).ifPresent(existingEmployee::setPermissions);

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

//        // Samo admin može brisati admina
//        if (employee.getPermissions().contains("admin") && !currentUserHasAdminPermission()) {
//            throw new AccessDeniedException("Samo admin može brisati drugog admina");
//        }

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

    private String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
}
