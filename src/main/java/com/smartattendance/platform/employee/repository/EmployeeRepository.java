package com.smartattendance.platform.employee.repository;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import com.smartattendance.platform.employee.entity.EmployeeRole;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByPublicId(UUID publicId);

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    long countByStatus(EmployeeStatus status);

    java.util.List<Employee> findByRole(EmployeeRole role);

    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    @Query("""
        SELECT e FROM Employee e
        WHERE (:status IS NULL OR e.status = :status)
          AND (:search IS NULL OR
               LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Employee> findAllWithFilters(@Param("status") EmployeeStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);
}
