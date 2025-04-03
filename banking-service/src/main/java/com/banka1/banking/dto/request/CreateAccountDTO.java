package com.banka1.banking.dto.request;

import com.banka1.banking.dto.CreateCompanyDTO;
import com.banka1.banking.models.helper.AccountStatus;
import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreateAccountDTO {
    @NotNull(message = "ID vlasnika racuna ne može biti prazan")
    private Long ownerID;

    @NotNull(message = "Izaberi valutu za racun")
    private CurrencyType currency;

    @NotNull(message = "Izaberi tip racuna")
    private AccountType type;

    @NotNull(message = "Izaberi podtip racuna")
    private AccountSubtype subtype;

    private Double dailyLimit;

    private Double monthlyLimit;

    @NotNull(message = "Izaberi status racuna")
    private AccountStatus status;

    @NotNull(message = "Izaberi da li da se kreiraju kartice za racun")
    private Boolean createCard;

    private Double balance;
    private CreateCompanyDTO companyData;
}
