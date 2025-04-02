package com.banka1.banking.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Receiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerAccountId;

    @Column(nullable = false)
    private String accountNumber;

    @Column()
    private String firstName;

    @Column()
    private String lastName;

    @Column()
    private String address;
}
