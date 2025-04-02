package com.banka1.user.DTO.response;

import com.banka1.common.model.Permission;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class CustomerResponse {
    @NonNull
    private Long id;

    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    private String username;

    @NonNull
    private String birthDate;

    @NonNull
    private Gender gender;

    @NonNull
    private String email;

    @NonNull
    private String phoneNumber;

    @NonNull
    private String address;

    @NonNull
    private List<Permission> permissions;
}
