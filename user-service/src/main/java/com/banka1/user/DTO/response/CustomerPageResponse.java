package com.banka1.user.DTO.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import java.util.List;

@Schema(description = "Stranica liste musterija.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPageResponse {
    @Schema(description = "Ukupan (za ceo rezultat, to jest iz svih stranica) broj rezultata.")
    long total;

    @Schema(description = "Podaci na ovoj stranici.")
    @NonNull
    List<CustomerResponse> rows;
}
