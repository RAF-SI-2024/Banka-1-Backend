package com.banka1.user.DTO.response;

import com.banka1.common.model.Permission;
import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import com.banka1.common.model.Position;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class EmployeeResponse {
    @NonNull
    private Long id;

    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    private String username;

    @NonNull
    private String birthDate; // Format: "YYYY-MM-DD"

    @NonNull
    private Gender gender;

    @NonNull
    private String email;

    @NonNull
    private String phoneNumber;

    @NonNull
    private String address;

    @NonNull
    private Position position;

    @NonNull
    private Department department;

    @NonNull
    private Boolean active;

    @NonNull
    private Boolean isAdmin;

    @NonNull
    private List<Permission> permissions;
}
