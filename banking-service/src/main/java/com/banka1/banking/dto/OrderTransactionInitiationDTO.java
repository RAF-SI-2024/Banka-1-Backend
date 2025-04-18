package com.banka1.banking.dto;

import lombok.Data;

@Data
public class OrderTransactionInitiationDTO {
    private String uid;
    private Long sellerAccountId;
    private Long buyerAccountId;
    private Double amount;
    private Double fee;
    private String direction; // "buy" ili "sell"
}
