package com.banka1.user.model;

import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String birthDate; // Format: "YYYY-MM-DD"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String username;

    @Column
    private String password;

    @Column
    private String saltPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position; // pozicija u banci, direktor, menadzer, radnik...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department; // odeljenje u banci, racunovodstvo, marketing, prodaja...

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean isAdmin;

    private String verificationCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "employee_permissions", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "permission")
    private List<Permission> permissions;
}
