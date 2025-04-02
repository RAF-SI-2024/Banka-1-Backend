package com.banka1.banking.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private Long transferId;

    @Column(nullable = false)
    private Long expirationTime;

    @Column(nullable = false)
    private boolean used = false;


}
