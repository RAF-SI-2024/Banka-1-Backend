package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.Transaction;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.InstallmentsRepository;
import com.banka1.banking.repository.LoanRepository;
import com.banka1.banking.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
@EnableScheduling

public class LoanService {
    private final LoanRepository loanRepository;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final ModelMapper modelMapper;
    private final String destinationEmail;
    private final AccountRepository accountRepository;
    private final UserServiceCustomer userServiceCustomer;
    private final InstallmentsRepository installmentsRepository;
    private final TransactionService transactionService;

    public LoanService(LoanRepository loanRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, ModelMapper modelMapper, @Value("${destination.email}") String destinationEmail, AccountRepository accountRepository, UserServiceCustomer userServiceCustomer, InstallmentsRepository installmentsRepository, TransactionService transactionService) {
        this.loanRepository = loanRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.modelMapper = modelMapper;
        this.destinationEmail = destinationEmail;
        this.accountRepository = accountRepository;
        this.userServiceCustomer = userServiceCustomer;
        this.installmentsRepository = installmentsRepository;
        this.transactionService = transactionService;
    }

    public Loan createLoan(@Valid CreateLoanDTO createLoanDTO) {
        System.out.println("===== DEBUG START =====");
        System.out.println("Creating loan for account: " + createLoanDTO.getAccountId());

        // Find account
        Account account = accountRepository.findById(createLoanDTO.getAccountId())
                .orElseThrow(() -> new RuntimeException("Racun nije pronadjen"));
    
        if (account == null) {return null;}
        
        // Create new loan manually
        Loan newLoan = new Loan();
        
        // Set basic loan info from DTO
        newLoan.setLoanType(createLoanDTO.getLoanType());
        newLoan.setNumberOfInstallments(createLoanDTO.getNumberOfInstallments());
        newLoan.setInterestType(createLoanDTO.getInterestType());
        newLoan.setLoanAmount(createLoanDTO.getLoanAmount());
        newLoan.setCurrencyType(createLoanDTO.getCurrencyType());
        
        // Set loan reason from loanPurpose
        if (createLoanDTO.getLoanPurpose() != null) {
            newLoan.setLoanReason(createLoanDTO.getLoanPurpose());
        } else {
            newLoan.setLoanReason("General purpose");
        }
        
        // CRITICAL - Set duration equal to numberOfInstallments
        newLoan.setDuration(createLoanDTO.getNumberOfInstallments());
        
        // Set rates with defaults
        newLoan.setNominalRate(createLoanDTO.getNominalRate() != null ? 
            createLoanDTO.getNominalRate() : 5.5);
        newLoan.setEffectiveRate(createLoanDTO.getEffectiveRate() != null ? 
            createLoanDTO.getEffectiveRate() : 6.0);
        
        // Set dates
        // Set dates with milliseconds instead of seconds for frontend compatibility
        long currentTimeSeconds = Instant.now().getEpochSecond();
        long currentTimeMillis = currentTimeSeconds * 1000; // Convert to milliseconds

        newLoan.setCreatedDate(currentTimeMillis);
        newLoan.setAllowedDate(createLoanDTO.getAllowedDate() != null ? 
            createLoanDTO.getAllowedDate() * 1000 : currentTimeMillis);
        newLoan.setNextPaymentDate(currentTimeMillis + 30*24*60*60*1000); // 30 days in milliseconds
        
        // Set payment info
        newLoan.setRemainingAmount(createLoanDTO.getLoanAmount());
        newLoan.setMonthlyPayment(createLoanDTO.getMonthlyPayment() != null ? 
            createLoanDTO.getMonthlyPayment() : 
            (createLoanDTO.getLoanAmount() / createLoanDTO.getNumberOfInstallments()) * 1.05);
        
        // Set status and account
        newLoan.setPaymentStatus(PaymentStatus.PENDING);
        newLoan.setAccount(account);
        
        // Validation before saving
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
        
        // Debug output to verify all fields are set
        System.out.println("SAVING LOAN: Duration=" + newLoan.getDuration() + 
                          ", Installments=" + newLoan.getNumberOfInstallments() + 
                          ", Type=" + newLoan.getLoanType());
        
        // Save and return the loan
        return loanRepository.save(newLoan);
    }

    public List<Loan> getPendingLoans() {
        return loanRepository.findByPaymentStatus(PaymentStatus.PENDING);
    }

    public List<Loan> getAllUserLoans(Long ownerId) {
        List<Account> accounts = accountRepository.findByOwnerID(ownerId);
        if (accounts.isEmpty()) {
            return null;
        }
        List<Loan> loans = new ArrayList<>();
        for (Account acc : accounts) {
            loans.addAll(getAllLoansForAccount(acc));
        }
        return loans;
    } //mozda da bude mapa sa parovima racun-lista kredita ?

    public List<Loan> getAllLoansForAccount(Account account) {
        return loanRepository.getLoansByAccount(account);
    }

    public Loan getLoanDetails(Long loanId) {
        return loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen"));
    }

    public Loan updateLoanRequest(Long loanId, LoanUpdateDTO loanUpdateDTO) {
        try {
            // Find the loan
            Loan loan = loanRepository.findById(loanId).orElse(null);
            if (loan == null) {return null;}
            
            // Update loan status based on approval decision
            String emailMessage = "";
            if (loanUpdateDTO.getApproved()) {
                loan.setPaymentStatus(PaymentStatus.APPROVED);
                emailMessage = "Vaš zahtev za kredit u iznosu " + loan.getLoanAmount() + 
                              " " + loan.getCurrencyType() + " je odobren.";
            } else {
                loan.setPaymentStatus(PaymentStatus.DENIED);
                emailMessage = "Vaš zahtev za kredit u iznosu " + loan.getLoanAmount() + 
                              " " + loan.getCurrencyType() + " je odbijen.";
                
                // Store rejection reason if provided
                if (loanUpdateDTO.getReason() != null && !loanUpdateDTO.getReason().isEmpty()) {
                    emailMessage += "\nRazlog: " + loanUpdateDTO.getReason();
                }
            }
            
            // Save the updated loan first
            Loan savedLoan = loanRepository.save(loan);
            
            // Direct DB approach to get user email
            Account acc = loan.getAccount();
            sendLoanNotification(acc, emailMessage, loanId);
            
            System.out.println("Loan " + loanId + " status updated to: " + 
                    (loanUpdateDTO.getApproved() ? "APPROVED" : "DENIED"));

//            acc.setBalance(acc.getBalance() + loan.getLoanAmount());
            
            return savedLoan;
        } catch (Exception e) {
            System.err.println("Error updating loan: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error updating loan: " + e.getMessage());
        }
    }
    
    private void sendLoanNotification(Account acc, String emailMessage, Long loanId) {
        try {
            // Create notification directly
            NotificationDTO emailDTO = new NotificationDTO();
            emailDTO.setSubject("Obaveštenje o statusu kredita");
            emailDTO.setMessage("Nije prosao loan jbg");
            emailDTO.setType("email");
            
            // Try getting customer from service first
            try {
                CustomerDTO customer = userServiceCustomer.getCustomerById(acc.getOwnerID());
                emailDTO.setEmail(customer.getEmail());
                
                // Send notification using JMS
                System.out.println("Sending loan notification for loan " + loanId + " to user ID " + customer.getEmail());
                jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));
                
            } catch (Exception e) {
                System.err.println("Failed to send notification: " + e.getMessage());
                // Still allow the loan update to succeed even if notification fails
            }
        } catch (Exception e) {
            System.err.println("Error in notification process: " + e.getMessage());
            // Don't propagate notification errors to the main workflow
        }
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Installment> getUserInstallments(Long userId) {
        List<Account> accounts = accountRepository.findByOwnerID(userId);
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Installment> installments = new ArrayList<>();
        for (Account acc : accounts) {
            List<Loan> loans = loanRepository.getLoansByAccount(acc);
            for (Loan loan : loans) {
                installments.addAll(installmentsRepository.getByLoanId(loan.getId()));
            }
        }
        return installments;
    }
    public Integer calculateRemainingInstallments(Long ownerId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen"));

        if (!loan.getAccount().getOwnerID().equals(ownerId)) {return null;}
        Integer paidInstallments = installmentsRepository.countByLoan(loan);
        Integer numberOfInstallments = loan.getNumberOfInstallments()-paidInstallments;
        return numberOfInstallments;
    }
    public Account getBankAccount(CurrencyType currencyType) {
        Long ownerId = 1L;
        return accountRepository.findByOwnerIDAndCurrencyType(ownerId, currencyType);
    }

    @Scheduled(cron = "0 0 0 * * *")  // Pokreće se svakog dana u ponoć
    @Transactional
    public void processLoanPayments() {
        processDueInstallments();
    }

    public void processDueInstallments() {
        List<Installment> dueInstallments = installmentsRepository.getDueInstallments(Instant.now().getEpochSecond());

        if (dueInstallments == null || dueInstallments.isEmpty()) {
            System.out.println("⚠️ No due installments found!");
            throw new RuntimeException("Nijedna rata nije danas na redu za naplatu");
        }

        for (Installment installment : dueInstallments) {
            if (installment == null) {
                System.out.println("⚠️ No installment info found!");
                continue;
            }

            Account customerAccount = installment.getLoan().getAccount();
            Account bankAccount = getBankAccount(customerAccount.getCurrencyType());

            Boolean successfull = transactionService.processInstallment(customerAccount, bankAccount, installment);

            if (successfull){
                // Naplata uspela
                installment.setIsPaid(true);
                installment.setActualDueDate(Instant.now().getEpochSecond());
            } else {
                // Naplata nije uspela
                installment.setIsPaid(false);

                if (installment.getAttemptCount() != 0) {
                    installment.setInterestRate(installment.getInterestRate() + 0.0005); // +0.05%
                }

                installment.setRetryDate(Instant.now().plus(Duration.ofDays(3)).getEpochSecond());
            }
            installmentsRepository.save(installment);
        }
    }
}
