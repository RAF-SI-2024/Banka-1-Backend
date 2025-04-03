package com.banka1.banking.dto;

import com.banka1.banking.dto.request.CreateAccountDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@RequiredArgsConstructor
@Data
public class CreateAccountByEmployeeDTO {
    @NonNull
    private CreateAccountDTO createAccountDTO;
    @NonNull
    private Long employeeId;
    private CreateCompanyDTO companyData;
}
