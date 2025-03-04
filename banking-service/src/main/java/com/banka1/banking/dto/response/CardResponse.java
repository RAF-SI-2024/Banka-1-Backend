package com.banka1.banking.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class CardResponse {
    @NonNull
    private AccountResponse account;
}
