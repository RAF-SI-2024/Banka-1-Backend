package com.banka1.banking.repository;

import com.banka1.banking.models.Receiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiverRepository extends JpaRepository<Receiver, Long> {

    //List<Receiver> findByOwnerAccountId(Long ownerAccountId);

    List<Receiver> findByCustomerIdOrderByUsageCountDesc(Long customerId);


    Optional<Receiver> findById(Long id);

   // boolean existsByOwnerAccountIdAndAccountNumber(Long ownerAccountId, String accountNumber);

    boolean existsByCustomerIdAndAccountNumber(Long customerId, String accountNumber);

    void deleteById(Long id);


}
