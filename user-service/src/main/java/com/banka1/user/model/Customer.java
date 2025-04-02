package com.banka1.user.model;

import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Customer {

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

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column
    private String password;

    @Column
    private String saltPassword;

    private String verificationCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "customer_permissions", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "permission")
    private List<Permission> permissions;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "customer_bank_accounts", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "bank_account")
    private List<String> bankAccounts; // trenutno se ne koristi, u drugom sprintu ce biti prebaceno u servis banke
}
