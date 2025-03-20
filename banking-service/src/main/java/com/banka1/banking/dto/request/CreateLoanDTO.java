package com.banka1.banking.dto.request;

import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLoanDTO {
    private String loanPurpose;
    private LoanType loanType;
    private Integer numberOfInstallments;
    private InterestType interestType;
    private Double loanAmount;
    private Double salaryAmount;
    private String employmentStatus;
    private Integer employmentDuration;  
    private String phoneNumber;
    private CurrencyType currencyType;
    
    // Optional fields from before
    private Double nominalRate;
    private Double effectiveRate;
    private Integer duration;
    private Long allowedDate;
    private Double monthlyPayment;
    
    @NotNull
    private Long accountId;
}
