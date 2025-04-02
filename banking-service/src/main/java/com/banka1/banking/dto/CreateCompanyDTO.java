package com.banka1.banking.dto;

import com.banka1.banking.models.helper.BusinessActivityCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class CreateCompanyDTO {

    private String name;

    private String address;

    @NotBlank(message = "VAT number is required")
    @Size(min = 9, max = 9, message = "VAT number must be exactly 9 characters")
    @Pattern(regexp = "\\d{9}", message = "VAT number must be numeric and exactly 9 digits")
    private String vatNumber;

    @NotBlank(message = "Company number is required")
    @Size(min = 8, max = 8, message = "Company number must be exactly 9 characters")
    @Pattern(regexp = "\\d{8}", message = "Company number must be numeric and exactly 9 digits")
    private String companyNumber;

    private BusinessActivityCode bas;

    private Long ownerId;
}
