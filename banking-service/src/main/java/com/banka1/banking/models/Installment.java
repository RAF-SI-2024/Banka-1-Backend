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
    private Long expectedDueDate; // Očekivani datum dospeća

    @Column
    private Long actualDueDate; // Pravi datum dospeća

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    @Column(nullable = false)
    private Boolean isPaid;
    @Column(nullable = false)
    private LocalDate retryDate; // Datum sledećeg pokušaja naplate
    @Column(nullable = false)
    private Integer attemptCount; // Broj pokušaja naplate

    @Column(nullable = false)
    private Boolean isPaid;

    @Column(nullable = false)
    private Long retryDate; // Datum sledećeg pokušaja naplate

    @Column(nullable = false)
    private Integer attemptCount; // Broj pokušaja naplate

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    Transaction transaction;

}
