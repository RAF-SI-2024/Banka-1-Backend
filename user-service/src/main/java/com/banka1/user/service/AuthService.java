package com.banka1.user.service;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.utils.ResponseMessage;
import com.banka1.common.model.Position;
import com.banka1.common.service.implementation.GenericAuthService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService extends GenericAuthService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public AuthService(CustomerRepository customerRepository, EmployeeRepository employeeRepository) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    // Provera validnosti lozinke koriscenjem BCrypt-a kombinovanjem sa salt-om.
    private boolean verifyPassword(String rawPassword, String hashedPassword, String salt){
        if (rawPassword == null || hashedPassword == null || salt == null)
            return false;
        return BCrypt.checkpw(rawPassword + salt, hashedPassword);
    }

    public String login(LoginRequest loginRequest) throws IllegalArgumentException {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if(customer != null && verifyPassword(password, customer.getPassword(), customer.getSaltPassword()))
            return generateToken(customer.getId(), Position.NONE, customer.getPermissions(), false, false, null);

        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        if(employee != null && verifyPassword(password, employee.getPassword(), employee.getSaltPassword()) && employee.getActive())
            return generateToken(employee.getId(), employee.getPosition(), employee.getPermissions(), true, employee.getIsAdmin(), employee.getDepartment());

        throw new IllegalArgumentException(ResponseMessage.FAILED_LOGIN.toString());
    }
}