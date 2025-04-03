package com.banka1.user.repository;

import com.banka1.common.model.Department;
import com.banka1.user.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);
    List<Employee> findByDepartment(Department department);



    boolean existsByEmail(String email);

    boolean existsById(@NonNull Long id);


    @Query("SELECT e FROM Employee e WHERE " +
            "(:firstName IS NULL OR e.firstName LIKE %:firstName%) AND " +
            "(:lastName IS NULL OR e.lastName LIKE %:lastName%) AND " +
            "(:email IS NULL OR e.email LIKE %:email%) AND " +
            "(:position IS NULL OR e.position = :position)")
    List<Employee> findFilteredEmployees(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("email") String email,
            @Param("position") String position
    );

    @Query("SELECT e FROM Employee e WHERE e.department = 'AGENT'")
    List<Employee> getActuaries();

}