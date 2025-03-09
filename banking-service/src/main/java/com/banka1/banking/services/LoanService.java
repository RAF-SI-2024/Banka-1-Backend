package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.LoanRepository;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final ModelMapper modelMapper;
    private final String destinationEmail;
    private final AccountRepository accountRepository;

    public LoanService(LoanRepository loanRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, ModelMapper modelMapper, @Value("${destination.email}") String destinationEmail, AccountRepository accountRepository) {
        this.loanRepository = loanRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.modelMapper = modelMapper;
        this.destinationEmail = destinationEmail;
        this.accountRepository = accountRepository;
    }

    public Loan createLoan(@Valid CreateLoanDTO createLoanDTO) {
        Account account = accountRepository.findById(createLoanDTO.getAccountId())
                .orElseThrow(() -> new RuntimeException("Racun nije pronadjen"));

        if (account == null) {return null;}

        Loan newLoan = modelMapper.map(createLoanDTO, Loan.class);

        if (newLoan.getLoanType().equals(LoanType.MORTGAGE)) {
            if (newLoan.getNumberOfInstallments() == null ||
                    (newLoan.getNumberOfInstallments()%60 != 0) ||
                        newLoan.getNumberOfInstallments() > 360) {
                return null;
            }
        } else {
            if (newLoan.getNumberOfInstallments() == null ||
                    (newLoan.getNumberOfInstallments()%12 != 0) ||
                        newLoan.getNumberOfInstallments() > 84) {
                return null;
            }
        }

        newLoan.setPaymentStatus(PaymentStatus.PENDING);
        newLoan.setAccount(account);
        newLoan.setCreatedDate(Instant.now().getEpochSecond());
        newLoan.setRemainingAmount(newLoan.getLoanAmount());
        newLoan.setNextPaymentDate(newLoan.getCreatedDate()+30*24*60*60);
//        newLoan.setCurrencyType(account.getCurrencyType());

        newLoan = loanRepository.save(newLoan);
        return newLoan;
    }

    public List<Loan> getPendingLoans() {
        return loanRepository.findByPaymentStatus(PaymentStatus.PENDING);
    }

    public List<Loan> getAllUserLoans(Long ownerId) {
        List<Account> accounts = accountRepository.findByOwnerID(ownerId);
        if (accounts.isEmpty()) {return null;}
        List<Loan> loans = new ArrayList<>();
        for (Account acc : accounts) {
            loans.addAll(getAllLoans(acc));
        }
        return loans;
    } //mozda da bude mapa sa parovima racun-lista kredita ?

    public List<Loan> getAllLoans(Account account) {
        return loanRepository.getLoansByAccount(account);
    }

    public Loan getLoanDetails(Long ownerId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen"));
        if (!loan.getAccount().getOwnerID().equals(ownerId)) {return null;}
        return loan;
    }

    public Loan updateLoanRequest(Long loanId, LoanUpdateDTO loanUpdateDTO) {
        Loan loan = loanRepository.getById(loanId);
        if (loan == null) {return null;}
        if (loanUpdateDTO.getApproved()) {
            loan.setPaymentStatus(PaymentStatus.APPROVED);
        } else {
            loan.setPaymentStatus(PaymentStatus.DENIED);
        }
        return loanRepository.save(loan);
    }
}
