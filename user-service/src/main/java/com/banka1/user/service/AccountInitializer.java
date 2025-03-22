package com.banka1.user.service;

import com.banka1.user.model.Customer;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.repository.CustomerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
//import com.banka1.common.model.Permission;
import java.util.List;

//@RequiredArgsConstructor
//@Service
public class AccountInitializer {

//    private final CustomerRepository customerRepository;
//
//    @PostConstruct
//    public void init(){
//        String email = "bankabanka@banka1.com";
//        String rawPassword = "Banka12345";
//        String salt = "salt";
//        String hashedPassword = BCrypt.hashpw(rawPassword+salt,BCrypt.gensalt());
//
//        if (!customerRepository.existsCustomerByEmail("bankabanka@banka1.com")){
//            Customer customer = new Customer();
//            customer.setFirstName("Banka");
//            customer.setLastName("Banka");
//            customer.setBirthDate("2025-01-01");
//            customer.setGender(Gender.MALE);
//            customer.setEmail(email);
//            customer.setUsername("bankabanka");
//            customer.setPhoneNumber("+381640000000");
//            customer.setAddress("Bulevar Banka 1");
//            customer.setSaltPassword(salt);
//            customer.setPassword(hashedPassword);
//            customer.setPermissions(List.of(Permission.SET_CUSTOMER_PERMISSION));
//            customerRepository.save(customer);
//        }
//
//    }

}
