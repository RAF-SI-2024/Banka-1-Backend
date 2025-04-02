package com.banka1.banking.dto.request;

import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLoanDTO {
    @NotNull
    private String loanPurpose;
    @NotNull
    private LoanType loanType;
    @NotNull
    private Integer numberOfInstallments;
    @NotNull
    private InterestType interestType;
    @NotNull
    private Double loanAmount;
    @NotNull
    private Double salaryAmount;
    @NotNull
    private String employmentStatus;
    @NotNull
    private Integer employmentDuration;
    @NotNull
    private  String phoneNumber;
    @NotNull
    private Long accountId;
}
