package com.banka1.user.bootstrap;

import com.banka1.user.model.Employee;
import com.banka1.user.model.helper.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.model.helper.Position;
import com.banka1.user.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class BootstapData implements CommandLineRunner {
    public final EmployeeRepository employeeRepository;

    @Autowired
    public BootstapData(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("============== Loading Data ==============");

        Employee admin = new Employee();

        String rawPassword = "admin";
        String salt = "salt";
        String hashedPassword = BCrypt.hashpw(rawPassword + salt,BCrypt.gensalt());
        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setEmail("admin@admin.com");
        admin.setPassword(hashedPassword);
        admin.setIsAdmin(true);
        admin.setPhoneNumber("1234567890");
        admin.setBirthDate("2000-01-01");
        admin.setGender(Gender.MALE);
        admin.setDepartment(Department.HR);
        admin.setPosition(Position.DIRECTOR);
        admin.setActive(true);
        admin.setAddress("Admin Address");
        admin.setSaltPassword(salt);
        admin.setUsername("admin123");

        employeeRepository.save(admin);

        System.out.println("============== Data Loaded ==============");
    }
}
