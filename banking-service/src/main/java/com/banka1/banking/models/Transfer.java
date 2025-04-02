package com.banka1.banking.models;

import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccountId;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccountId;

    @Column(nullable = false)
    private Double amount;

    @Column()
    private String receiver;

    @Column()
    private String adress;

    @Column()
    private String paymentCode; // sifra placanja

    @Column()
    private String paymentReference; // poziv na broj

    @Column()
    private String paymentDescription; // svrha placanja

    @ManyToOne
    @JoinColumn(name = "from_currency_id", nullable = false)
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "to_currency_id", nullable = false)
    private Currency toCurrency;

    @Column()
    private Long createdAt;

    @Column()
    private String otp;

    @Column()
    @Enumerated(EnumType.STRING)
    private TransferType type;

    @Column()
    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @Column()
    private Long completedAt;

    @Column()
    private String note;
}
