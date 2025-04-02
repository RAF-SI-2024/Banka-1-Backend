package com.banka1.notification.service;

import com.banka1.notification.model.CustomerDevice;
import com.banka1.notification.repository.CustomerDeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    CustomerDeviceRepository customerDeviceRepository;

    public DeviceService(CustomerDeviceRepository customerDeviceRepository) {
        this.customerDeviceRepository = customerDeviceRepository;
    }

    public void deleteDevice(Long customerId, String deviceToken) {
        customerDeviceRepository.deleteByCustomerIdAndDeviceToken(customerId, deviceToken);
    }

    public void deleteAllDevices(Long customerId) {
        customerDeviceRepository.deleteByCustomerId(customerId);
    }

    public void addDevice(Long customerId, String deviceToken) {

        CustomerDevice customerDevice = new CustomerDevice();
        customerDevice.setCustomerId(customerId);
        customerDevice.setDeviceToken(deviceToken);

        customerDeviceRepository.save(customerDevice);
    }
}
