package com.banka1.banking.controller;

import com.banka1.banking.controllers.ReceiverController;
import com.banka1.banking.dto.ReceiverDTO;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.services.ReceiverService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiverControllerTest {

    @Mock
    ReceiverService receiverService;

    @InjectMocks
    ReceiverController receiverController;

    @Test
    void testAddReceiver_Success() {
        ReceiverDTO receiverDTO = new ReceiverDTO();
        Receiver receiver = new Receiver();

        when(receiverService.createReceiver(receiverDTO)).thenReturn(receiver);

        ResponseEntity<?> response = receiverController.addReceivers(receiverDTO);

        assertEquals(200, response.getStatusCodeValue());
        verify(receiverService).createReceiver(receiverDTO);
    }
}
