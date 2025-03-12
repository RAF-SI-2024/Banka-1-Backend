package com.banka1.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangePreviewDTO {
    private String fromCurrency;
    private String toCurrency;
    private Double amount;
}
