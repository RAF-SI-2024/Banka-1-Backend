package com.banka1.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class CustomerDevice {
    @Id
    private Long id;

    private String deviceToken;

    private Long customerId;
}
