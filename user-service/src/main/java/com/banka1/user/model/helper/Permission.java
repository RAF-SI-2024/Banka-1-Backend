package com.banka1.user.model.helper;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Permission {
    CREATE_EMPLOYEE("user.employee.create"),
    EDIT_EMPLOYEE("user.employee.edit"),
    DELETE_EMPLOYEE("user.employee.delete"),
    LIST_EMPLOYEE("user.employee.list"),
    READ_EMPLOYEE("user.employee.view"),
    SET_EMPLOYEE_PERMISSION("user.employee.permission"),
    CREATE_CUSTOMER("user.customer.create"),
    EDIT_CUSTOMER("user.customer.edit"),
    DELETE_CUSTOMER("user.customer.delete"),
    LIST_CUSTOMER("user.customer.list"),
    READ_CUSTOMER("user.customer.view"),
    SET_CUSTOMER_PERMISSION("user.customer.permission");
    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    @Override
    @JsonValue
    public String toString() {
        return getPermission();
    }
}
