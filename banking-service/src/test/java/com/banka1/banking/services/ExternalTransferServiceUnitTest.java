package com.banka1.banking.services;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.Transfer;
import com.banka1.banking.models.helper.TransferStatus;
import com.banka1.banking.models.helper.TransferType;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.TransactionRepository;
import com.banka1.banking.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalTransferServiceUnitTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransferService transferService;

    private Transfer transfer;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(5000.0);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(3000.0);

        transfer = new Transfer();
        transfer.setId(100L);
        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(2000.0);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setType(TransferType.EXTERNAL);
    }

    @Test
    @Transactional
    void shouldProcessExternalTransferSuccessfully() {
        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount, toAccount);

        String result = transferService.processTransfer(100L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transferRepository, times(1)).save(transfer);
    }

    @Test
    @Transactional
    void shouldFailExternalTransferWhenInsufficientBalance() {
        transfer.setAmount(6000.0);
        when(transferRepository.findById(100L)).thenReturn(Optional.of(transfer));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> transferService.processTransfer(100L));
        assertEquals("Insufficient balance for transfer", exception.getMessage());
        assertEquals(TransferStatus.FAILED, transfer.getStatus());
        verify(transferRepository, times(1)).save(transfer);
    }
}
