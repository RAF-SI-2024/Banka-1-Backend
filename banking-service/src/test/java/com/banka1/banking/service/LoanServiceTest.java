package com.banka1.banking.service;

import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.models.Account;

import com.banka1.banking.models.Loan;

import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.LoanRepository;
import com.banka1.banking.services.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {
    
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private LoanService loanService;

    private CreateLoanDTO createLoanDTO;
    @BeforeEach
    void setUp() {
        createLoanDTO = new CreateLoanDTO();
        createLoanDTO.setAccountId(1L);
        createLoanDTO.setMonthlyPayment(10.0);
        createLoanDTO.setDuration(12);
        createLoanDTO.setLoanAmount(20000.0);
        createLoanDTO.setEffectiveRate(0.4);
        createLoanDTO.setNominalRate(0.21);
        createLoanDTO.setInterestType(InterestType.FIXED);
        createLoanDTO.setNumberOfInstallments(12);
        createLoanDTO.setLoanType(LoanType.CASH);
        createLoanDTO.setCurrencyType(CurrencyType.RSD); // Add this line
    }
    @Test
    void testCreateLoanSuccessfully() {
        Account account = new Account();
        account.setId(1L);
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.PERSONAL);

        when(accountRepository.findById(createLoanDTO.getAccountId())).thenReturn(Optional.of(account));

        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.createLoan(createLoanDTO);

        assertNotNull(result);
        assertEquals(createLoanDTO.getLoanType(), result.getLoanType());
        assertEquals(createLoanDTO.getNumberOfInstallments(), result.getNumberOfInstallments());
        assertEquals(createLoanDTO.getInterestType(), result.getInterestType());
        assertEquals(createLoanDTO.getLoanAmount(), result.getLoanAmount());
        assertEquals(createLoanDTO.getNominalRate(), result.getNominalRate());
        assertEquals(createLoanDTO.getEffectiveRate(), result.getEffectiveRate());
        assertEquals(createLoanDTO.getNumberOfInstallments(), result.getDuration());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        assertEquals(account, result.getAccount());

        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void testCreateLoanAccountNotFound() {
        when(accountRepository.findById(createLoanDTO.getAccountId())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            loanService.createLoan(createLoanDTO);
        });
        assertEquals("Racun nije pronadjen", exception.getMessage());

        verify(loanRepository, never()).save(any());
    }
    
}
