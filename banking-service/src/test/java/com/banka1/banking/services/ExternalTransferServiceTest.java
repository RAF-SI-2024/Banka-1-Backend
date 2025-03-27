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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExternalTransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    private Transfer transfer;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setBalance(1000.0);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setBalance(500.0);

        transfer = new Transfer();
        transfer.setId(1L);
        transfer.setFromAccountId(fromAccount);
        transfer.setToAccountId(toAccount);
        transfer.setAmount(200.0);
        transfer.setType(TransferType.INTERNAL);
        transfer.setStatus(TransferStatus.PENDING);
    }

    @Test
    void testProcessInternalTransfer_Success() {
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        String result = transferService.processInternalTransfer(1L);

        assertEquals("Transfer completed successfully", result);
        assertEquals(800.0, fromAccount.getBalance());
        assertEquals(700.0, toAccount.getBalance());
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transferRepository, times(1)).save(transfer);
    }

    @Test
    void testProcessInternalTransfer_FailedDueToInsufficientFunds() {
        transfer.setAmount(1500.0); // Više nego što ima na računu
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        Exception exception = assertThrows(RuntimeException.class, () -> transferService.processInternalTransfer(1L));

        assertEquals("Insufficient funds", exception.getMessage());
        assertEquals(TransferStatus.FAILED, transfer.getStatus());

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transferRepository, times(1)).save(transfer);
    }

    @Test
    void testProcessInternalTransfer_FailedDueToInvalidStatus() {
        transfer.setStatus(TransferStatus.COMPLETED);
        when(transferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        Exception exception = assertThrows(RuntimeException.class, () -> transferService.processInternalTransfer(1L));

        assertEquals("Transfer is not in pending state", exception.getMessage());

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(transferRepository, never()).save(transfer);
    }
}