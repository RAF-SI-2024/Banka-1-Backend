package com.banka1.banking.repository;

import com.banka1.banking.models.Account;
import com.banka1.banking.models.Loan;
import com.banka1.banking.models.helper.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Loan> getLoansByAccount(Account account);
}
