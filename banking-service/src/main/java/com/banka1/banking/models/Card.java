package com.banka1.banking.models;

import com.banka1.banking.models.helper.CardBrand;
import com.banka1.banking.models.helper.CardType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String cardName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardBrand cardBrand; // VISA, MASTERCARD, AMERICAN_EXPRESS

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CardType cardType; // DEBIT, CREDIT

    @Column(nullable = false)
    private String cardCvv;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private Long createdAt;

    @Column(nullable = false)
    private Long expirationDate;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean blocked;

    @Column(nullable = false)
    private Double cardLimit;

    @ManyToOne
    @JoinColumn(name = "authorized_person_id")
    private AuthorizedPerson authorizedPerson;
}
