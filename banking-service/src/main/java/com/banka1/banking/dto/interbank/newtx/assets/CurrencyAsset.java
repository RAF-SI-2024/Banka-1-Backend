package com.banka1.banking.dto.interbank.newtx.assets;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrencyAsset {
    private String currency; // npr. "EUR", "USD", ...
}
