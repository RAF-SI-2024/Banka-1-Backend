package com.banka1.banking.models;

import com.banka1.banking.models.helper.BusinessActivityCode;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column private Long id;

    @Column private String name;

    @Column private String address;

    @Column private String vatNumber;

    @Column private String companyNumber;

    @Enumerated(EnumType.STRING)
    @Column private BusinessActivityCode bas;

    @Column private Long ownerID;

}
