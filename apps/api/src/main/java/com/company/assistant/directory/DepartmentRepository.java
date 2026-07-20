package com.company.assistant.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    @Query("""
        SELECT d FROM Department d
        WHERE :search IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        """)
    Page<Department> search(@Param("search") String search, Pageable pageable);
}
