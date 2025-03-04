package com.banka1.user.repository;

import com.banka1.user.model.ResetPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResetPasswordRepository extends JpaRepository<ResetPassword, Long> {
    ResetPassword findByToken(String token);
}
