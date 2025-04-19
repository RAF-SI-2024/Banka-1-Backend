package com.banka1.banking.models.helper;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
@AllArgsConstructor
public class IdempotenceKey {

    private Integer routingNumber;

    @Column(length = 64)
    private String locallyGeneratedKey;

    public IdempotenceKey() {

    }
}