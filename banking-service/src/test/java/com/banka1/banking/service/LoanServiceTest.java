package com.banka1.banking.service;

import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.models.Account;

import com.banka1.banking.models.Loan;

import com.banka1.banking.models.helper.AccountSubtype;
import com.banka1.banking.models.helper.AccountType;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
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
import static org.mockito.Mockito.never;

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
        createLoanDTO.setDuration(12.0);
        createLoanDTO.setLoanAmount(20000.0);
        createLoanDTO.setEffectiveRate(0.4);
        createLoanDTO.setNominalRate(0.21);
        createLoanDTO.setInterestType(InterestType.FIXED);
        createLoanDTO.setNumberOfInstallments(12);
        createLoanDTO.setLoanType(LoanType.CASH);
    }
    @Test
    void testCreateLoanSuccessfully() {

        Loan loan = new Loan();
        loan.setLoanType(LoanType.CASH);
        loan.setNumberOfInstallments(12);

        when(modelMapper.map(createLoanDTO, Loan.class)).thenReturn(loan);

        Account account = new Account();
        account.setType(AccountType.CURRENT);
        account.setSubtype(AccountSubtype.PERSONAL);

        when(accountRepository.findById(createLoanDTO.getAccountId())).thenReturn(Optional.of(account));

        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.createLoan(createLoanDTO);

        assertNotNull(result);
        verify(loanRepository, times(1)).save(loan);
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
