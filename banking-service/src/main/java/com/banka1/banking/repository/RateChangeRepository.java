package com.banka1.banking.repository;

import com.banka1.banking.models.RateChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RateChangeRepository extends JpaRepository<RateChange,Long> {
}