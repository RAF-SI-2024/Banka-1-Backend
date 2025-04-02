package com.banka1.notification.sender;

import com.banka1.notification.DTO.response.NotificationDTO;

public interface NotificationSender {
    void sendToCustomer(NotificationDTO notification);
}
