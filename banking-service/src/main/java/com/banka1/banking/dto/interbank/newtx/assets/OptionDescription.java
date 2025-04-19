package com.banka1.banking.dto.interbank.newtx.assets;

import lombok.Data;

@Data
public class OptionDescription {
    private int id;
    private StockDescription stock;
    private double pricePerUnit;
    private String settlementDate;
    private int amount;
}
