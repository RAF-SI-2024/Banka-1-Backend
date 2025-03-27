package com.banka1.banking.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalTransferDTO {

    private Long fromAccountId;
    private Long toAccountId;
    private Double amount;

}
