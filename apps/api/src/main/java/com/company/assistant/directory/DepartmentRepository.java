package com.company.assistant.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    /**
     * A-34 (#194): kelime basi eslesmesi ve deterministik siralama — calisan aramasiyla ayni
     * kural (bkz. {@link EmployeeRepository#search}). Birini duzeltip digerini birakmak, ayni
     * ekran ailesinde iki farkli arama davranisi demek olurdu.
     */
    @Query("""
        SELECT d FROM Department d
        WHERE :search IS NULL
           OR LOWER(d.name) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
           OR LOWER(d.name) LIKE LOWER(CONCAT('% ', CAST(:search AS string), '%'))
        ORDER BY d.name, d.id
        """)
    Page<Department> search(@Param("search") String search, Pageable pageable);

    /**
     * A-19 (#129): yalnizca departman adlari. RuleBasedIntentMatcher'in yoneticiye ihtiyaci
     * yok; searchDepartments() DepartmentResponse kurarken lazy manager proxy'sini aciyor ve
     * HTTP istegi disinda (or. IT) LazyInitializationException veriyor. Ad projeksiyonu hem
     * bu bagimliligi kaldirir hem daha ucuzdur.
     */
    @Query("SELECT d.name FROM Department d")
    List<String> findAllNames();
}
