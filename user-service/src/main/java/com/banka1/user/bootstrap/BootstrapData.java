package com.banka1.user.bootstrap;

import com.banka1.user.DTO.CustomerDTO.CustomerDTO;
import com.banka1.user.DTO.request.CreateEmployeeDto;
import com.banka1.user.model.Customer;
import com.banka1.user.model.Employee;
import com.banka1.user.model.helper.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.user.model.helper.Permission;
import com.banka1.user.model.helper.Position;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BootstrapData implements CommandLineRunner {
    public final EmployeeRepository employeeRepository;
    public final CustomerRepository customerRepository;

    @Autowired
    public BootstrapData(EmployeeRepository employeeRepository, CustomerRepository customerRepository) {
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("============== Loading Data ==============");

        Employee admin = new Employee();

        String rawPassword = "admin";
        String salt = "salt";
        String hashedPassword = BCrypt.hashpw(rawPassword + salt, BCrypt.gensalt());

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

        admin.setPermissions(List.of(Permission.CREATE_EMPLOYEE, Permission.READ_EMPLOYEE));

        employeeRepository.save(admin);

        var musterija = new Customer();

        musterija.setFirstName("musterija");
        musterija.setLastName("musterija");
        musterija.setEmail("musterija@musterija.com");
        musterija.setPassword("musterija");
        musterija.setPhoneNumber("1234567890");
        musterija.setBirthDate(20000101L);
        musterija.setGender(Gender.MALE);
        musterija.setAddress("musterija Address");
        musterija.setSaltPassword("salt");
        musterija.setUsername("musterija123");

        //customerRepository.save(musterija);

        Employee employee1 = new Employee();
        String rawPassword1 = "Per@12345";
        String salt1 = "salt1";
        String hashedPassword1 = BCrypt.hashpw(rawPassword1 + salt1, BCrypt.gensalt());

        employee1.setFirstName("Petar");
        employee1.setLastName("Petrović");
        employee1.setEmail("petar.petrovic@banka.com");
        employee1.setUsername("perica");
        employee1.setPhoneNumber("+381641001000");
        employee1.setBirthDate("1990-07-07");
        employee1.setAddress("Knez Mihailova 5");
        employee1.setGender(Gender.MALE);
        employee1.setPosition(Position.MANAGER);
        employee1.setDepartment(Department.IT);
        employee1.setActive(true);
        employee1.setIsAdmin(false);
        employee1.setPermissions(List.of(Permission.CREATE_CUSTOMER,Permission.DELETE_CUSTOMER,Permission.LIST_CUSTOMER,Permission.EDIT_CUSTOMER,Permission.READ_CUSTOMER,Permission.SET_CUSTOMER_PERMISSION,Permission.SET_EMPLOYEE_PERMISSION,Permission.DELETE_EMPLOYEE,Permission.EDIT_EMPLOYEE,Permission.LIST_EMPLOYEE,Permission.READ_EMPLOYEE,Permission.CREATE_EMPLOYEE));
        employee1.setSaltPassword(salt1);
        employee1.setPassword(hashedPassword1);

        employeeRepository.save(employee1);

        Employee employee2 = new Employee();
        String rawPassword2 = "Jovan@12345";
        String salt2 = "salt2";
        String hashedPassword2 = BCrypt.hashpw(rawPassword2 + salt2, BCrypt.gensalt());

        employee2.setFirstName("Jovana");
        employee2.setLastName("Jovanović");
        employee2.setEmail("jovana.jovanovic@banka.com");
        employee1.setUsername("jjovanaa");
        employee2.setPhoneNumber("+381641001001");
        employee2.setBirthDate("2000-10-10");
        employee2.setAddress("Knez Mihailova 6");
        employee2.setGender(Gender.FEMALE);
        employee2.setPosition(Position.WORKER);
        employee2.setDepartment(Department.HR);
        employee2.setActive(true);
        employee2.setIsAdmin(false);
        employee2.setPermissions(List.of(Permission.READ_CUSTOMER,Permission.CREATE_CUSTOMER,Permission.DELETE_CUSTOMER,Permission.LIST_CUSTOMER,Permission.EDIT_CUSTOMER));
        employee2.setSaltPassword(salt2);
        employee2.setPassword(hashedPassword2);

        employeeRepository.save(employee2);

        Customer customer1 = new Customer();
        String rawPassword3 = "M@rko12345";
        String salt3 = "salt3";
        String hashedPassword3 = BCrypt.hashpw(rawPassword3 + salt3, BCrypt.gensalt());

        customer1.setFirstName("Marko");
        customer1.setLastName("Marković");
        customer1.setEmail("marko.markovic@banka.com");
        customer1.setUsername("okram");
        customer1.setPhoneNumber("+381641001002");
        customer1.setBirthDate(20051212L);
        customer1.setGender(Gender.MALE);
        customer1.setAddress("Knez Mihailova 7");
        customer1.setPermissions(List.of(Permission.READ_EMPLOYEE));
        customer1.setSaltPassword(salt3);
        customer1.setPassword(hashedPassword3);

        customerRepository.save(customer1);

        Customer customer2 = new Customer();
        String rawPassword4 = "Anastas12345";
        String salt4 = "salt4";
        String hashedPassword4 = BCrypt.hashpw(rawPassword4 + salt4, BCrypt.gensalt());

        customer2.setFirstName("Anastasija");
        customer2.setLastName("Milinković");
        customer2.setEmail("anastasija.milinkovic@banka.com");
        customer2.setUsername("anastass");
        customer2.setPhoneNumber("+381641001003");
        customer2.setBirthDate(20010202L);
        customer2.setGender(Gender.FEMALE);
        customer2.setAddress("Knez Mihailova 8");
        customer2.setPermissions(List.of(Permission.READ_EMPLOYEE));
        customer2.setSaltPassword(salt4);
        customer2.setPassword(hashedPassword4);

        customerRepository.save(customer2);

        System.out.println("============== Data Loaded ==============");
    }
}
