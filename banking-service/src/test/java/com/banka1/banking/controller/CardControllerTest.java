package com.banka1.banking.controller;

import com.banka1.banking.controllers.CardController;
import com.banka1.banking.dto.CreateCardDTO;
import com.banka1.banking.models.Card;
import com.banka1.banking.services.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    CardService cardService;

    @InjectMocks
    CardController cardController;

    @Test
    void testCreateCard_Success() {
        CreateCardDTO createCardDTO = new CreateCardDTO();
        Card card = new Card();
        card.setId(1L);
        
        when(cardService.createCard(createCardDTO)).thenReturn(card);

        ResponseEntity<?> response = cardController.createCard(createCardDTO);
        
        assertEquals(201, response.getStatusCodeValue());
        verify(cardService).createCard(createCardDTO);
    }
}