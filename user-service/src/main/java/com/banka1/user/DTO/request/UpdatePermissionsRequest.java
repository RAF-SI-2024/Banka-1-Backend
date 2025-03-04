package com.banka1.user.DTO.request;

import com.banka1.user.model.helper.Permission;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePermissionsRequest {
    @NotNull
    private List<Permission> permissions;
}
