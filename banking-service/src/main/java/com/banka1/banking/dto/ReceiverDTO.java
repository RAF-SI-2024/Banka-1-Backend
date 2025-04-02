package com.banka1.banking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiverDTO {

    private Long ownerAccountId;

    private String accountNumber;

    private String fullName;

    private String address;

}
