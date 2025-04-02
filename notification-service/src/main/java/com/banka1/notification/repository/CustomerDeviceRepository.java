package com.banka1.notification.repository;

import com.banka1.notification.model.CustomerDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerDeviceRepository extends JpaRepository<CustomerDevice, Long> {
    void deleteByCustomerIdAndDeviceToken(Long customerId, String deviceId);
    void deleteByCustomerId(Long customerId);
    List<CustomerDevice> findByCustomerId(Long customerId);
}
