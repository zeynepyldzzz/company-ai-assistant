package com.company.assistant.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  java.util.Optional<Employee> findByEmail(String email);

    // #84: soft-delete edilen (active=false) calisanlar listelerde gorunmemeli.
    @Query("""
        SELECT e FROM Employee e
        WHERE e.active = true
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:department IS NULL OR LOWER(e.department.name) LIKE LOWER(CONCAT('%', CAST(:department AS string), '%')))
          AND (:office IS NULL OR e.officeStatus = CAST(:office AS string))
        """)
    Page<Employee> search(
            @Param("search") String search,
            @Param("department") String department,
            @Param("office") String office,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM Employee e
        WHERE e.active = true
          AND e.phone IS NOT NULL
          AND (:search IS NULL
               OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR e.phone LIKE CONCAT('%', CAST(:search AS string), '%'))
        """)
    Page<Employee> searchPhonebook(@Param("search") String search, Pageable pageable);

    Optional<Employee> findByPhone(String phone);

    boolean existsByDepartment_Id(Integer departmentId);
}
