package com.banka1.banking.models;

import com.banka1.banking.models.helper.AccountStatus;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerID;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private Double balance;

    @Column(nullable = false)
    private Double reservedBalance;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyType currencyType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountSubtype subtype;

    @Column(nullable = false)
    private Long createdDate;

    @Column(nullable = false)
    private Long expirationDate;

    @Column(nullable = false)
    private Double dailyLimit;

    @Column(nullable = false)
    private Double monthlyLimit;

    @Column(nullable = false)
    private Double dailySpent;

    @Column(nullable = false)
    private Double monthlySpent;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(nullable = false)
    private Long employeeID;

    @Column(nullable = false)
    private Double monthlyMaintenanceFee;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

}
