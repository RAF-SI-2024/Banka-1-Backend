package com.banka1.user.DTO.request;

import com.banka1.common.model.Department;
import com.banka1.user.model.helper.Gender;
import com.banka1.common.model.Position;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private Position position;
    private String username;
    private String email;
    private String birthDate;
    private Department department;
    private Boolean active;
    private Boolean isAdmin;
    private Gender gender;
}
