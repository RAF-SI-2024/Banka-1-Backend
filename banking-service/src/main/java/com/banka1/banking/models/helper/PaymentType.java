package com.banka1.banking.models.helper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentType {
    private String code;
    private String description;

    public PaymentType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
