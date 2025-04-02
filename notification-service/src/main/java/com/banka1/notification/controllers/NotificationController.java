package com.banka1.notification.controllers;

import com.banka1.notification.DTO.request.AddDeviceDTO;
import com.banka1.notification.aspect.Authorization;
import com.banka1.notification.service.DeviceService;
import com.banka1.notification.service.implementation.AuthService;
import com.banka1.notification.utils.ResponseTemplate;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@Tag(name = "Customer API", description = "API za upravljanje notifikacijama")
public class NotificationController {

    private final AuthService authService;
    private final DeviceService deviceService;

    public NotificationController(AuthService authService, DeviceService deviceService) {
        this.authService = authService;
        this.deviceService = deviceService;
    }

    @PostMapping("/add-device")
    @Authorization
    public ResponseEntity<?> addDevice(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody AddDeviceDTO addDeviceDTO) {
        Long customerId = authService.parseToken(authService.getToken(authorization)).get("id", Long.class);

        deviceService.addDevice(customerId, addDeviceDTO.deviceToken);

        return ResponseTemplate.create(ResponseEntity.status(HttpStatus.OK), true, null, null);
    }
}
