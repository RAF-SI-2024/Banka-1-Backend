package com.banka1.banking.repository;
import com.banka1.banking.models.AuthorizedPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuthorizedPersonRepository extends JpaRepository<AuthorizedPerson, Long> {

    Optional<AuthorizedPerson> findByFirstNameAndLastNameAndPhoneNumber(String firstName, String lastName, String phoneNumber);

}
