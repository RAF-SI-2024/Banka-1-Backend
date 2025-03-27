package com.banka1.banking.models;

import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount; // Iznos rate

    @Column(nullable = false)
    private Double interestRate; // Iznos kamatne stope

    @Column(nullable = false)
    private CurrencyType currencyType; // Valuta rate i kredita

    @Column(nullable = false)
    private LocalDate expectedDueDate; // Očekivani datum dospeća

    @Column
    private Long actualDueDate; // Pravi datum dospeća

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    @Column(nullable = false)
    private Boolean isPaid = false;
    @Column
    private LocalDate retryDate; // Datum sledećeg pokušaja naplate
    @Column(nullable = false)
    private Integer attemptCount = 0; // Broj pokušaja naplate

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private boolean lawsuit = false;

    @ManyToOne
    @JoinColumn(name = "transaction_id")
    Transaction transaction;

}
