package com.banka1.banking.dto.request;

import lombok.Data;

@Data
public class LoanUpdateDTO {
    private Boolean approved;
    private String reason; // Optional, useful for rejections

}
