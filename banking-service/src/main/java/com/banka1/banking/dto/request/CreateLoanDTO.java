package com.banka1.banking.dto.request;

import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLoanDTO {
    private String loanReason;
    private LoanType loanType;
    private Integer numberOfInstallments;
    private InterestType interestType;
    private Double nominalRate;
    private Double effectiveRate;
    private Double loanAmount;
    private Integer duration;
    private Long allowedDate;
    private Double monthlyPayment;
    private CurrencyType currencyType;
    @NotNull
    private Long accountId; //ne mozemo bas da trazimo Account type ali mozemo da povezemo sa Acc repo i da vadimo odatle
}
