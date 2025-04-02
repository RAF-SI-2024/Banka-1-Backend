package com.banka1.banking.controller;

import com.banka1.banking.controllers.ExchangeController;
import com.banka1.banking.dto.ExchangeMoneyTransferDTO;
import com.banka1.banking.services.ExchangeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeControllerTest {

    @Mock
    ExchangeService exchangeService;

    @InjectMocks
    ExchangeController exchangeController;

    @Test
    void testExchangeMoneyTransfer_Success() {
        ExchangeMoneyTransferDTO dto = new ExchangeMoneyTransferDTO();
        dto.setAmount(100.0);
        
        when(exchangeService.validateExchangeTransfer(dto)).thenReturn(true);
        when(exchangeService.createExchangeTransfer(dto)).thenReturn(123L);

        ResponseEntity<?> response = exchangeController.exchangeMoneyTransfer(dto);
        
        assertEquals(200, response.getStatusCodeValue());
        verify(exchangeService).validateExchangeTransfer(dto);
        verify(exchangeService).createExchangeTransfer(dto);
    }
}