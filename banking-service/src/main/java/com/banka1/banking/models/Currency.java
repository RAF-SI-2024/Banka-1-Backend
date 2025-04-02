package com.banka1.banking.models;
import com.banka1.banking.models.helper.CurrencyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Automatski generisan ID
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)  // Valutni kod mora biti jedinstven
    private CurrencyType code;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String symbol;
}
