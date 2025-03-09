package com.banka1.banking.models;

import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.models.helper.LoanType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    @Column
    private Integer numberOfInstallments;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CurrencyType currencyType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InterestType interestType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private Double nominalRate;

    @Column(nullable = false)
    private Double effectiveRate;

    @Column(nullable = false)
    private Double loanAmount;

    @Column(nullable = false)
    private Integer duration; // in months

    @Column(nullable = false)
    private Long createdDate;

    @Column(nullable = false)
    private Long allowedDate;

    @Column(nullable = false)
    private Double monthlyPayment;

    @Column(nullable = false)
    private Long nextPaymentDate;

    @Column(nullable = false)
    private Double remainingAmount;

    @Column
    private String loanReason;
    @JoinColumn(name = "account_id", nullable = false)
    @ManyToOne
    private Account account;
}
