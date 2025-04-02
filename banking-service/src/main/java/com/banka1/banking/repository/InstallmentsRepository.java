package com.banka1.banking.repository;

import com.banka1.banking.models.Installment;
import com.banka1.banking.models.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface InstallmentsRepository extends JpaRepository<Installment, Long> {
    List<Installment> getByLoanId(Long loanID);
    @Query("SELECT i FROM Installment i " +
            "WHERE (i.expectedDueDate <= :today AND i.isPaid = false AND i.lawsuit = false) " +
            "OR (i.retryDate IS NOT NULL AND i.retryDate <= :today AND i.isPaid = false AND i.lawsuit = false)")
    List<Installment> getDueInstallments(@Param("today") LocalDate today);

}
