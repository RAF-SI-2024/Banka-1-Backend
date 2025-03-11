package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.helper.CurrencyType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.InstallmentRepository;
import com.banka1.banking.repository.LoanRepository;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final ModelMapper modelMapper;
    private final String destinationEmail;
    private final AccountRepository accountRepository;
    private final UserServiceCustomer userServiceCustomer;
    private final InstallmentRepository installmentRepository;

    public LoanService(LoanRepository loanRepository, JmsTemplate jmsTemplate, MessageHelper messageHelper, ModelMapper modelMapper, @Value("${destination.email}") String destinationEmail, AccountRepository accountRepository, UserServiceCustomer userServiceCustomer, InstallmentRepository installmentRepository) {
        this.loanRepository = loanRepository;
        this.jmsTemplate = jmsTemplate;
        this.messageHelper = messageHelper;
        this.modelMapper = modelMapper;
        this.destinationEmail = destinationEmail;
        this.accountRepository = accountRepository;
        this.userServiceCustomer = userServiceCustomer;
        this.installmentRepository = installmentRepository;
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
        Loan loan = loanRepository.findById(loanId).orElse(null);
        if (loan == null) {return null;}
        String message = "";
        if (loanUpdateDTO.getApproved()) {
            loan.setPaymentStatus(PaymentStatus.APPROVED);
            message = "Vaš kredit je odobren.";
        } else {
            loan.setPaymentStatus(PaymentStatus.DENIED);
            message = "Vaš kredit je odbijen.";
        }

        CustomerDTO owner = userServiceCustomer.getCustomerById(loan.getAccount().getOwnerID());
        NotificationDTO emailDTO = new NotificationDTO();
        emailDTO.setSubject("Promena statusa kredita");
        emailDTO.setEmail(owner.getEmail());
        emailDTO.setMessage(message+"\n"+loanUpdateDTO.getReason());
        emailDTO.setFirstName(owner.getFirstName());
        emailDTO.setLastName(owner.getLastName());
        emailDTO.setType("email");

        jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));

        return loanRepository.save(loan);
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
                installments.addAll(installmentRepository.getByLoanId(loan.getId()));
            }
        }
        return installments;
    }
    public Integer calculateRemainingInstallments(Long ownerId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen"));

        if (!loan.getAccount().getOwnerID().equals(ownerId)) {return null;}
        Integer paidInstallments = installmentRepository.countByLoan(loan);
        Integer numberOfInstallments = loan.getNumberOfInstallments()-paidInstallments;
        return numberOfInstallments;
    }

    public Account getBankAccount(CurrencyType currencyType) {
        Long ownerId = 1L; // The bank's owner ID is always 1

        return accountRepository.findByOwnerIDAndCurrencyType(ownerId, currencyType);
    }

//    @Autowired
//    TransactionRepository transactionRepository;
//    @Autowired
//    TransactionService transactionService;
//
//    @Scheduled(cron = "0 0 0 * * *")  // Pokreće se svakog dana u ponoć
//    public void processLoanPayments() {
//        processDueInstallments();
//    }
//
//    @Transactional
//    public void processDueInstallments() {
//        List<Installment> dueInstallments = installmentsRepository.getDueInstallments(Instant.now().getEpochSecond());
//
//        for (Installment installment : dueInstallments) {
//            Account customerAccount = installment.getLoan().getAccount();
//            Account bankAccount = getBankAccount(customerAccount.getCurrencyType());
//
//            Boolean successfull = transactionService.processInstallment(customerAccount, bankAccount, installment);
//
//            if (successfull){
//                // Naplata uspešna
//                installment.setIsPaid(true);
//                installment.setActualDueDate(Instant.now().getEpochSecond());
//                installment.setAttemptCount(installment.getAttemptCount() + 1);
//            }
//            else {
//                // Naplata nije uspela
//                installment.setIsPaid(false);
//                if (installment.getAttemptCount() != 0) {
//                    installment.setInterestRate(installment.getInterestRate() + 0.0005); // +0.05%
//                }
//
//                installment.setRetryDate(Instant.now().plus(Duration.ofDays(3)).getEpochSecond());
//                installment.setAttemptCount(installment.getAttemptCount() + 1);
//
//            }
//
//            installmentsRepository.save(installment);
//        }
//    }
}
