package com.banka1.banking.models;

import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    @Column(nullable = false)
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
    private Double penalty = 0.0;

    @Column(nullable = false)
    private Double loanAmount;

    @Column(nullable = false)
    private Long createdDate;

    @Column
    private Long allowedDate;

    @Column(nullable = false)
    private Double monthlyPayment;

    @Column(nullable = false)
    private Integer numberOfPaidInstallments = 0;

    @Column(nullable = false)
    private LocalDate nextPaymentDate;

    @Column(nullable = false)
    private Double remainingAmount;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String loanReason;

    @JoinColumn(name = "account_id", nullable = false)
    @ManyToOne
    private Account account;
}
