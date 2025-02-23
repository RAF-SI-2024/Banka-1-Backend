package com.banka1.user.service;

import com.banka1.user.DTO.request.LoginRequest;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import com.banka1.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(CustomerRepository customerRepository,EmployeeRepository employeeRepository,JwtUtil jwtUtil){
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
    }

    // Login metoda omogucava korisnicima da se prijave koristeci email i lozinku.
    // Prvo trazi korisnika kao customer,ako nije pronadjen trazi korisnika kao employee,u suprotnom baca izuzetak.
    // AKO JE ULOGOVANI KORISNIK ADMIN,TRENUTNO MU SE DODELJUJE ROLA EMPLOYEE,ALI AKO TREBA DA BUDE ADMIN
    // POSTOJI ZAKOMENTARISANA LINIJA KOJU TREBA UBACITI NA MESTO ROLE-A U DRUGOJ IF PETLJI
    public String login(LoginRequest loginRequest){
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        Customer customer = customerRepository.findByEmail(email).orElse(null);

        if(customer != null && verifyPassword(password,customer.getPassword(),customer.getSaltPassword())){
            return jwtUtil.generateToken(
                    customer.getId(),
                    "CUSTOMER",
                    customer.getPermissions().stream().map(Enum::name).toList()
            );
        }

        Employee employee = employeeRepository.findByEmail(email).orElse(null);

        if(employee != null && verifyPassword(password,employee.getPassword(),employee.getSaltPassword())){
            //String role = employee.getIsAdmin() ? "ADMIN" : "EMPLOYEE";
            return jwtUtil.generateToken(
                    employee.getId(),
                    "EMPLOYEE",
                    employee.getPermissions().stream().map(Enum::name).toList()
            );
        }

        throw new RuntimeException("Korisnik ne postoji.");
    }

    // Provera validnosti lozinke koriscenjem BCrypt-a kombinovanjem sa salt-om.
    private boolean verifyPassword(String rawPassword,String hashedPassword,String salt){
        return BCrypt.checkpw(rawPassword + salt , hashedPassword);
    }

}