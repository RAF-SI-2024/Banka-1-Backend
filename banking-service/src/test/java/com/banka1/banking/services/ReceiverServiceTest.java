package com.banka1.banking.services;

import com.banka1.banking.dto.ReceiverDTO;
import com.banka1.banking.models.Receiver;
import com.banka1.banking.repository.ReceiverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiverServiceTest {

    @Mock
    private ReceiverRepository receiverRepository;

    @InjectMocks
    private ReceiverService receiverService;

    private ReceiverDTO receiverDTO;
    private Receiver receiver;

    @BeforeEach
    void setUp() {
        receiverDTO = new ReceiverDTO();
        receiverDTO.setCustomerId(1L);
        receiverDTO.setAccountNumber("123-456789");
        receiverDTO.setFullName("Marko Marković");
        receiverDTO.setAddress("Kralja Petra 12");

        receiver = new Receiver();
        receiver.setId(1L);
        receiver.setCustomerId(1L);
        receiver.setAccountNumber("123-456789");
        receiver.setFirstName("Marko");
        receiver.setLastName("Marković");
        receiver.setAddress("Kralja Petra 12");
    }

    @Test
    void createReceiverShouldSaveReceiverWhenNewReceiver() {
        when(receiverRepository.existsByCustomerIdAndAccountNumber(1L, "123-456789")).thenReturn(false);
        when(receiverRepository.save(any(Receiver.class))).thenReturn(receiver);

        Receiver savedReceiver = receiverService.createReceiver(receiverDTO);

        assertNotNull(savedReceiver);
        assertEquals("Marko", savedReceiver.getFirstName());
        assertEquals("Marković", savedReceiver.getLastName());
        verify(receiverRepository, times(1)).save(any(Receiver.class));
    }

    @Test
    void createReceiverShouldThrowExceptionWhenReceiverAlreadyExists() {
        when(receiverRepository.existsByCustomerIdAndAccountNumber(1L, "123-456789")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> receiverService.createReceiver(receiverDTO));
    }

    @Test
    void getReceiversByAccountIdShouldReturnReceivers() {
        when(receiverRepository.findByCustomerIdOrderByUsageCountDesc(1L)).thenReturn(List.of(receiver));

        List<Receiver> receivers = receiverService.getReceiversByCustomerId(1L);

        assertEquals(1, receivers.size());
        assertEquals("Marko", receivers.get(0).getFirstName());
    }

    @Test
    void updateReceiverShouldUpdateReceiverWhenExists() {
        when(receiverRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(receiverRepository.save(any(Receiver.class))).thenReturn(receiver);

        receiverDTO.setFullName("Nikola Nikolić");
        Receiver updatedReceiver = receiverService.updateReceiver(1L, receiverDTO);

        assertEquals("Nikola", updatedReceiver.getFirstName());
        assertEquals("Nikolić", updatedReceiver.getLastName());
        verify(receiverRepository, times(1)).save(receiver);
    }

    @Test
    void updateReceiverShouldThrowExceptionWhenReceiverNotFound() {
        when(receiverRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> receiverService.updateReceiver(1L, receiverDTO));
    }

    @Test
    void deleteReceiverShouldDeleteWhenReceiverExists() {
        when(receiverRepository.existsById(1L)).thenReturn(true);
        doNothing().when(receiverRepository).deleteById(1L);

        receiverService.deleteReceiver(1L);

        verify(receiverRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteReceiverShouldThrowExceptionWhenReceiverNotFound() {
        when(receiverRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> receiverService.deleteReceiver(1L));
    }
}
