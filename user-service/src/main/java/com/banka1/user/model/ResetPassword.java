package com.banka1.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ResetPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false) // CUSTOMER, EMPLOYEE
    private Integer type;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private Long expirationDate;

    @Column(nullable = false)
    private Boolean used;

    @Column(nullable = false)
    private Long createdDate;
}
