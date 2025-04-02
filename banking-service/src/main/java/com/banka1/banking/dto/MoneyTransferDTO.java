package com.banka1.banking.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoneyTransferDTO {

    private String fromAccountNumber;
    private String recipientAccount;
    private Double amount;
    private String receiver;
    private String adress;
    private String payementCode;
    private String payementReference;
    private String payementDescription;

}
