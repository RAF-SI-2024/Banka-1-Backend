package com.banka1.user.service;


import com.banka1.user.DTO.banking.CreateAccountByEmployeeDTO;
import com.banka1.common.model.Permission;
import com.banka1.user.DTO.banking.CreateAccountDTO;
import com.banka1.user.DTO.request.CreateCustomerRequest;
import com.banka1.user.DTO.request.NotificationRequest;
import com.banka1.user.DTO.request.UpdateCustomerRequest;
import com.banka1.user.DTO.response.CustomerPageResponse;
import com.banka1.user.DTO.response.CustomerResponse;
import com.banka1.user.listener.MessageHelper;
import com.banka1.user.mapper.CustomerMapper;
import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

@Service
@Slf4j
@Tag(name = "Customer Service", description = "Business logic for customer operations")
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final SetPasswordService setPasswordService;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;

    @Value("${destination.email}")
    private String destinationEmail;
    @Value("${destination.account}")
    private String destinationAccount;
    @Value("${frontend.url}")
    private String frontendUrl;

    public CustomerResponse findById(String id) {
        return findById(Long.parseLong(id));
    }

    public CustomerResponse findById(long id) {
        var customerOptional = customerRepository.findById(id);
        if (customerOptional.isEmpty())
            return null;
        var customer = customerOptional.get();
        return CustomerMapper.customerToDto(customer);
    }

    public CustomerResponse findByEmail(String email) {
        var customerOptional = customerRepository.findByEmail(email);
        if(customerOptional.isEmpty())
            return null;
        var customer = customerOptional.get();
        return CustomerMapper.customerToDto(customer);
    }


    public CustomerPageResponse search(int page, int pageSize, Optional<String> sortField, Optional<String> sortOrder, Optional<String> filterField, Optional<String> filterValueOptional) {
        var direction = Sort.Direction.ASC;
        if (sortOrder.isPresent()) {
            switch (sortOrder.get().toLowerCase()) {
                case "asc" -> {}
                case "desc" -> direction = Sort.Direction.DESC;
                default -> throw new RuntimeException("Smer sortiranja nije prepoznat.");
            }
        }

        var pageRequest = PageRequest.of(page, pageSize, Sort.by(direction, sortField.orElse("id")));
        Page<Customer> customerPage;

        if (filterField.isPresent()) {
            var matcher = ExampleMatcher.matching().withMatcher(filterField.get(), contains());
            if (filterValueOptional.isEmpty())
                throw new RuntimeException("Specificirano polje za filtriranje ali nije specificirana vrednost.");
            var filterValue = filterValueOptional.get();
            var customer = new Customer();
            switch (filterField.get()) {
                case "id" -> customer.setId(Long.valueOf(filterValue));
                case "firstName" -> customer.setFirstName(filterValue);
                case "lastName" -> customer.setLastName(filterValue);
                case "birthDate" -> customer.setBirthDate(filterValue);
                case "gender" -> customer.setGender(Gender.valueOf(filterValue.toUpperCase()));
                case "email" -> customer.setEmail(filterValue);
                case "phoneNumber" -> customer.setPhoneNumber(filterValue);
                case "address" -> customer.setAddress(filterValue);
                default -> throw new RuntimeException("Polje za filtriranje nije prepoznato.");
            }
            var example = Example.of(customer, matcher);
            customerPage = customerRepository.findAll(example, pageRequest);
        }
        else
            customerPage = customerRepository.findAll(pageRequest);
        return new CustomerPageResponse(
                customerPage.getTotalElements(),
                customerPage.stream().map(CustomerMapper::customerToDto).toList()
        );
    }

    /**
     * Creates a new customer.
     * @param customerDTO The customer data transfer object.
     * @return The created customer entity.
     */
    public Customer createCustomer(CreateCustomerRequest customerDTO, Long employeeId) {
        Customer customer = CustomerMapper.dtoToCustomer(customerDTO);

        String verificationCode = UUID.randomUUID().toString();
        customer.setVerificationCode(verificationCode);

        NotificationRequest emailDTO = new NotificationRequest();
        emailDTO.setSubject("Nalog uspešno kreiran");
        emailDTO.setEmail(customer.getEmail());
        emailDTO.setMessage("Vaš nalog je uspešno kreiran. Kliknite na sledeći link da biste postavili lozinku: "
                + frontendUrl + "/set-password?token=" + verificationCode);
        emailDTO.setFirstName(customer.getFirstName());
        emailDTO.setLastName(customer.getLastName());
        emailDTO.setType("email");


        // Saving the customer in the database gives it an ID, which can be used to generate the set-password token
        customer = customerRepository.save(customer);

        jmsTemplate.convertAndSend(destinationAccount, messageHelper.createTextMessage(new CreateAccountByEmployeeDTO(new CreateAccountDTO(customerDTO.getAccountInfo(), customer.getId()), employeeId)));

        setPasswordService.saveSetPasswordRequest(verificationCode, customer.getId(), true);

        jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));
        return customer;
    }

    /**
     * Updates an existing customer's details.
     * @param id The customer ID.
     * @param customerDTO The updated customer data.
     * @return The updated customer, if found.
     */
    public Optional<Customer> updateCustomer(Long id, UpdateCustomerRequest customerDTO) {
        return customerRepository.findById(id).map(customer -> {
            if (customerDTO.getFirstName() != null) {
                customer.setFirstName(customerDTO.getFirstName());
            }
            if (customerDTO.getLastName() != null) {
                customer.setLastName(customerDTO.getLastName());
            }
            if (customerDTO.getBirthDate() != null) {
                customer.setBirthDate(customerDTO.getBirthDate());
            }
            if (customerDTO.getGender() != null) {
                customer.setGender(customerDTO.getGender());
            }
            if (customerDTO.getEmail() != null) {
                customer.setEmail(customerDTO.getEmail());
            }
            if (customerDTO.getPhoneNumber() != null) {
                customer.setPhoneNumber(customerDTO.getPhoneNumber());
            }
            if (customerDTO.getAddress() != null) {
                customer.setAddress(customerDTO.getAddress());
            }

            String verificationCode = UUID.randomUUID().toString();
            customer.setVerificationCode(verificationCode);

            System.out.println("Verification code: " + verificationCode);

            return customerRepository.save(customer);
        });
    }

    /**
     * Deletes a customer by ID.
     * @param id The customer ID.
     * @return True if the customer was deleted, otherwise throws an exception.
     */
    public boolean deleteCustomer(Long id) {
        Optional<Customer> customer = customerRepository.findById(id);

        if(customer.isEmpty()) {
            return false;
        }

        customerRepository.delete(customer.get());
        return true;
    }

    /**
     * Updates the permissions of a customer.
     * @param id The customer ID.
     * @param permissions The list of permissions to assign.
     * @return The updated customer with new permissions.
     */
    public Optional<Customer> updateCustomerPermissions(Long id, List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista permisija ne može biti prazna ili null");
        }

        return customerRepository.findById(id).map(customer -> {
            customer.setPermissions(permissions);
            return customerRepository.save(customer);
        });
    }
}