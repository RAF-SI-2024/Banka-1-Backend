package com.banka1.banking.services;

import com.banka1.banking.dto.CustomerDTO;
import com.banka1.banking.dto.MoneyTransferDTO;
import com.banka1.banking.dto.NotificationDTO;
import com.banka1.banking.dto.request.CreateLoanDTO;
import com.banka1.banking.dto.request.LoanUpdateDTO;
import com.banka1.banking.listener.MessageHelper;
import com.banka1.banking.models.Account;
import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.RateChange;
import com.banka1.banking.models.helper.InterestType;
import com.banka1.banking.models.helper.LoanType;
import com.banka1.banking.models.helper.PaymentStatus;
import com.banka1.banking.repository.AccountRepository;
import com.banka1.banking.repository.InstallmentsRepository;
import com.banka1.banking.repository.LoanRepository;
import com.banka1.banking.repository.RateChangeRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Service
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;
    private final JmsTemplate jmsTemplate;
    private final MessageHelper messageHelper;
    private final AccountRepository accountRepository;
    private final UserServiceCustomer userServiceCustomer;
    private final InstallmentsRepository installmentsRepository;
    private final TransactionService transactionService;
    private final BankAccountUtils bankAccountUtils;
    private final TransferService transferService;
    private final RateChangeRepository rateChangeRepository;
    private final UserServiceEmployee userServiceEmployee;

    @Value("${destination.email}")
    private String destinationEmail;

    public Loan createLoan(@Valid CreateLoanDTO createLoanDTO) {
        log.debug("Creating loan for account: {}", createLoanDTO.getAccountId());

        // Find account
        var account = accountRepository.findById(createLoanDTO.getAccountId())
                .orElseThrow(() -> new RuntimeException("Racun nije pronadjen"));

        if (account == null) {return null;}

        // Create new loan manually
        var loan = new Loan();
        loan.setAccount(account);

        // Set basic loan info from DTO
        loan.setLoanType(createLoanDTO.getLoanType());
        loan.setNumberOfInstallments(createLoanDTO.getNumberOfInstallments());
        loan.setInterestType(createLoanDTO.getInterestType());
        loan.setLoanAmount(createLoanDTO.getLoanAmount());
        loan.setCurrencyType(account.getCurrencyType());
        loan.setPhoneNumber(createLoanDTO.getPhoneNumber());
        loan.setLoanReason(createLoanDTO.getLoanPurpose());

        loan.setEffectiveRate(4.45);
        updateLoanRate(loan, false);
        loan.setNominalRate(loan.getEffectiveRate());


        // Set dates with milliseconds instead of seconds for frontend compatibility
        var currentTime = Instant.now();

        loan.setCreatedDate(currentTime.getEpochSecond());
        loan.setNextPaymentDate(LocalDate.ofInstant(currentTime, ZoneId.systemDefault()).plusMonths(1));

        loan.setRemainingAmount(createLoanDTO.getLoanAmount());
        loan.setPaymentStatus(PaymentStatus.PENDING);

        // Validation before saving
        if (loan.getLoanType().equals(LoanType.MORTGAGE)) {
            if (loan.getNumberOfInstallments() == null ||
                    (loan.getNumberOfInstallments()%60 != 0) ||
                    loan.getNumberOfInstallments() > 360) {
                return null;
            }
        } else {
            if (loan.getNumberOfInstallments() == null ||
                    (loan.getNumberOfInstallments()%12 != 0) ||
                    loan.getNumberOfInstallments() > 84) {
                return null;
            }
        }

        // Save and return the loan
        return loanRepository.save(loan);
    }

    public double calculateFixedRate(double amount) {
        if (amount <= 500000)
            return 6.25;
        if (amount <= 1000000)
            return 6;
        if (amount <= 2000000)
            return 5.75;
        if (amount <= 5000000)
            return 5.5;
        if (amount <= 10000000)
            return 5.25;
        if (amount <= 20000000)
            return 5;
        return 4.75;
    }

    public double calculateMargin(LoanType loanType) {
        switch (loanType) {
            case CASH -> {
                return 1.75;
            }
            case MORTGAGE -> {
                return 1.5;
            }
            case AUTO -> {
                return 1.25;
            }
            case REFINANCING -> {
                return 1;
            }
            case STUDENT -> {
                return 0.75;
            }
        }
        throw new RuntimeException("Nepoznat tip kredita");
    }

    public double getRateChange() {
        var date = LocalDate.now();

        var changeOpt = rateChangeRepository.findByYearAndMonth(date.getYear(), date.getMonthValue());

        if (changeOpt.isEmpty()) {
            var change = new RateChange();
            change.setYear(date.getYear());
            change.setMonth(date.getMonthValue());
            change.setChange((Math.random() - 0.5) * 3);
            return rateChangeRepository.save(change).getChange();
        }
        var change = changeOpt.get();


        return change.getChange();
    }

    public void calculateRemaining(Loan loan) {
        loan.setRemainingAmount((loan.getNumberOfInstallments() - loan.getNumberOfPaidInstallments()) * loan.getMonthlyPayment());
    }

    public Loan updateLoanRate(Loan loan, boolean save) {
        var rate = calculateFixedRate(loan.getLoanAmount());
        if (loan.getInterestType() == InterestType.VARIABLE)
            rate += getRateChange();
        rate += calculateMargin(loan.getLoanType()) + loan.getPenalty();
        if (rate != loan.getEffectiveRate()) {
            loan.setEffectiveRate(rate);
            loan.setMonthlyPayment(calculateInstallment(loan.getLoanAmount(), loan.getEffectiveRate() / 12, loan.getNumberOfInstallments()));
            calculateRemaining(loan);
            if (save)
                return loanRepository.save(loan);
        }
        return loan;
    }

    public List<Loan> getPendingLoans() {
        return loanRepository.findByPaymentStatus(PaymentStatus.PENDING).stream().map(loan -> updateLoanRate(loan, true)).toList();
    }

    public List<Loan> getAllUserLoans(Long ownerId) {
        List<Account> accounts = accountRepository.findByOwnerID(ownerId);
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<Loan> loans = new ArrayList<>();
        for (Account acc : accounts) {
            loans.addAll(getAllLoansForAccount(acc));
        }
        return loans.stream().map(loan -> updateLoanRate(loan, true)).toList();
    } //mozda da bude mapa sa parovima racun-lista kredita ?

    public List<Loan> getAllLoansForAccount(Account account) {
        return loanRepository.getLoansByAccount(account).stream().map(loan -> updateLoanRate(loan, true)).toList();
    }

    public Loan getLoanDetails(Long loanId) {
        return updateLoanRate(
                loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen")),
                true
        );
    }

    public Loan updateLoanRequest(Long loanId, LoanUpdateDTO loanUpdateDTO) {
        try {
            // Find the loan
            Loan loan = loanRepository.findById(loanId).orElse(null);
            if (loan == null) {return null;}

            if (loan.getPaymentStatus() != PaymentStatus.PENDING)
                return null;

            // Update loan status based on approval decision
            String emailMessage;
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
            loan = loanRepository.save(loan);

            // Direct DB approach to get user email
            Account acc = loan.getAccount();
            sendLoanNotification(acc, emailMessage, loanId);

            log.info("Loan {} status updated to: {}", loanId, loanUpdateDTO.getApproved() ? "APPROVED" : "DENIED");

            var customer = userServiceCustomer.getCustomerById(acc.getOwnerID());

            if(loanUpdateDTO.getApproved()) {
                updateLoanRate(loan, false);

                var bankAccount = bankAccountUtils.getBankAccountForCurrency(loan.getCurrencyType());
                var transferDTO = new MoneyTransferDTO(
                        bankAccount.getAccountNumber(),
                        acc.getAccountNumber(),
                        loan.getLoanAmount(),
                        customer.getFirstName() + " "  + customer.getLastName(),
                        customer.getAddress(),
                        "271",
                        "Kredit ID " + loan.getId(),
                        "Kredit"
                );

                if (!transferService.validateMoneyTransfer(transferDTO)) {
                    log.error("Kreiran nevalidan transfer tokom isplate kredita");
                    throw new RuntimeException("Interna greska: kreirani transfer nije bio validan.");
                }

                var transfer = transferService.createMoneyTransferEntity(
                        bankAccount,
                        acc,
                        transferDTO);

                transferService.processExternalTransfer(transfer.getId());

                createNextInstallment(loan, 0);

                loan.setAllowedDate(Instant.now().getEpochSecond());

                return loanRepository.save(loan);
            }

            return loan;
        } catch (Exception e) {
            log.warn("Error updating loan: {}", e.getMessage());
            throw new RuntimeException("Error updating loan: " + e.getMessage());
        }
    }

    private void sendLoanNotification(Account acc, String emailMessage, Long loanId) {
        try {
            NotificationDTO emailDTO = new NotificationDTO();
            emailDTO.setSubject("Obaveštenje o statusu kredita");
            emailDTO.setMessage(emailMessage);
            emailDTO.setType("email");

            try {
                CustomerDTO customer = userServiceCustomer.getCustomerById(acc.getOwnerID());
                emailDTO.setEmail(customer.getEmail());

                log.info("Sending loan notification for loan {} to user ID {}", loanId, customer.getEmail());
                jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));

            } catch (Exception e) {
	            log.warn("Failed to send notification: {}", e.getMessage());
            }
        } catch (Exception e) {
	        log.warn("Error in notification process: {}", e.getMessage());
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
        return installments.stream().map(installment -> {
            if (installment.getPaymentStatus() != PaymentStatus.PENDING)
                return installment;
            var loan = updateLoanRate(installment.getLoan(), true);
            if (Objects.equals(installment.getAmount(), loan.getMonthlyPayment()))
                return installment;
            updateInstallmentRate(installment);
            return installmentsRepository.save(installment);
        }).toList();
    }
    public Integer calculateRemainingInstallments(Long ownerId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).
                orElseThrow(() -> new RuntimeException("Kredit nije pronadjen"));

        if (!loan.getAccount().getOwnerID().equals(ownerId)) {return null;}
	    return loan.getNumberOfInstallments()-loan.getNumberOfPaidInstallments();
    }

    @Scheduled(cron = "0 0 0 * * *")  // Pokreće se svakog dana u ponoć
    @Transactional
    public void processLoanPayments() {
        processDueInstallments();
    }

    public void processDueInstallments() {
        List<Installment> dueInstallments = installmentsRepository.getDueInstallments(LocalDate.now());
        // the emojis will be preserved for posterity
        if (dueInstallments == null || dueInstallments.isEmpty()) {
            log.info("️⚠️ No loan installments to process");
            return;
        }

        dueInstallments.forEach(this::processDueInstallment);
    }

    private void processDueInstallment(Installment installment) {
        Account customerAccount = installment.getLoan().getAccount();
        Account bankAccount = bankAccountUtils.getBankAccountForCurrency(installment.getCurrencyType());

        try {
            var loan = updateLoanRate(installment.getLoan(), false);
            updateInstallmentRate(installment);
            var successful = processInstallment(customerAccount, bankAccount, installment);

            if (successful) {
                // Naplata uspela
                installment.setIsPaid(true);
                installment.setPaymentStatus(PaymentStatus.PAID_OFF);
                installment.setActualDueDate(Instant.now().getEpochSecond());

                loan.setNumberOfPaidInstallments(loan.getNumberOfPaidInstallments() + 1);
                calculateRemaining(loan);

                if (Objects.equals(loan.getNumberOfInstallments(), loan.getNumberOfPaidInstallments())) {
                    loan.setPaymentStatus(PaymentStatus.PAID_OFF);
                } else {
                    loan.setNextPaymentDate(loan.getNextPaymentDate().plusMonths(1));
                    createNextInstallment(loan, installment.getInstallmentNumber() + 1);
                }

            } else {
                // Naplata nije uspela
                installment.setIsPaid(false);
                installment.setPaymentStatus(PaymentStatus.LATE);
                installment.setAttemptCount(installment.getAttemptCount() + 1);
                installment.setRetryDate(LocalDate.now().plusDays(3));

                if (installment.getAttemptCount() >= 2) {
                    if (installment.getAmount() >= 100000) {
                        installment.setLawsuit(true);

                        NotificationDTO emailDTO = new NotificationDTO();
                        emailDTO.setSubject("Obaveštenje neplacenom kreditu");
                        emailDTO.setMessage("Kredit ID-a " + loan.getId() + " ima zakasnelu ratu koja je veca od 100000.");
                        emailDTO.setType("email");
                        var employee = userServiceEmployee.getEmployeeInLegal();
                        emailDTO.setEmail(employee.getEmail());

                        log.info("Sending lawsuit notification for loan {} to user email {}", loan.getId(), employee.getEmail());
                        jmsTemplate.convertAndSend(destinationEmail, messageHelper.createTextMessage(emailDTO));

                    } else {
                        loan.setPenalty(loan.getPenalty() + 0.05); // +0.05%
                        updateLoanRate(loan, false);
                        updateInstallmentRate(installment);
                        sendLoanNotification(customerAccount, "Rata kredita nije placena zbog nedostatka sredstava na racunu i godisnja kamatna stopa kredita je povecana za 0.05%. Ponovo ce biti naplacena za 3 dana. Ako ni tada ne bude dovoljno sredstava, kamata kredita ce se povecati.", loan.getId());
                    }
                } else {
                    sendLoanNotification(customerAccount, "Rata kredita nije placena zbog nedostatka sredstava na racunu. Ponovo ce biti naplacena za 3 dana. Ako ni tada ne bude dovoljno sredstava, kamata kredita ce se povecati.", loan.getId());
                }
            }

            loanRepository.save(loan);
            installmentsRepository.save(installment);
        } catch (Exception e) {
            log.error("Greska tokom isplate rate kredita {}: {}", installment.getId(), e.getMessage());
        }
    }

    public Boolean processInstallment(Account customerAccount, Account bankAccount, Installment installment) {
        var amount = installment.getAmount();

        if (customerAccount.getBalance().compareTo(amount) >= 0) {
            var transferDTO = new MoneyTransferDTO(
                    customerAccount.getAccountNumber(),
                    bankAccount.getAccountNumber(),
                    amount,
                    "Banka 1",
                    "Adresa",
                    "277",
                    "Rata ID " + installment.getId(),
                    "Rata kredita"
            );

            if (!transferService.validateMoneyTransfer(transferDTO))
                throw new RuntimeException("Kreiran nevalidan transfer");

            var transfer = transferService.createMoneyTransferEntity(customerAccount, bankAccount, transferDTO);

            transferService.processExternalTransfer(transfer.getId());

            installment.setTransaction(transactionService.findByTransfer(transfer));

            return true;
        }
        return false;
    }

    public double calculateInstallment(double loanAmount, double monthlyInterestRate, int numberOfInstallments) {
        monthlyInterestRate = monthlyInterestRate / 100;  // Kamatna stopa kao decimalni broj

        // Ako je mesečna kamatna stopa 0 (kredit bez kamate)
        if (monthlyInterestRate == 0) {
            return loanAmount / numberOfInstallments; // Ako nema kamate, rata je samo podeljeni iznos kredita
        }

        // Izračunavanje mesečne rate koristeći formulu
        var pow = Math.pow(1 + monthlyInterestRate, numberOfInstallments);
        var numerator = monthlyInterestRate * pow;
        var denominator = pow - 1;

        return loanAmount * (numerator / denominator);
    }

    public void updateInstallmentRate(Installment installment) {
        var loan = installment.getLoan();
        installment.setInterestRate(loan.getEffectiveRate() / 12);
        installment.setAmount(loan.getMonthlyPayment());
    }

    public void createNextInstallment(Loan loan, int installmentNumber) {
        var installment = new Installment();
        installment.setCurrencyType(loan.getCurrencyType());
        installment.setLoan(loan);
        installment.setInstallmentNumber(installmentNumber);
        installment.setExpectedDueDate(loan.getNextPaymentDate());
        installment.setInterestRate(loan.getEffectiveRate() / 12);
        installment.setAmount(loan.getMonthlyPayment());

        installmentsRepository.save(installment);
    }
}
