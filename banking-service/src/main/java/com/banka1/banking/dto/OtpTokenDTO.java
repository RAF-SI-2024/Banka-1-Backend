package com.banka1.banking.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OtpTokenDTO {

    private Long transferId;
    private String otpCode;

}

