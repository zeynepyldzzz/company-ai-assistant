package com.company.assistant.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  java.util.Optional<Employee> findByEmail(String email);

    /**
     * #84: soft-delete edilen (active=false) calisanlar listelerde gorunmemeli.
     *
     * <p>A-30 (#185): <b>LEFT JOIN sart.</b> Onceden filtre {@code e.department.name} uzerinden
     * yaziliyordu; JPQL'de bu yazim ORTUK INNER JOIN uretir ve departmani olmayan calisanlar
     * — filtre uygulanmasa bile — sonuctan tamamen elenir. Olculdu: 13 aktif calisanin 3'u
     * hicbir listede gorunmuyordu.
     *
     * <p>Etkisi rehber ekraniyla sinirli degildi: bu sorgu chatbot resolver'larinin da veri
     * kaynagi, dolayisiyla "sirkette kimler ofiste" sayilari eksik donuyor ve departmansiz
     * bir calisan isimle arandiginda bulunamiyordu. Ayrica o kisiler listede gorunmedigi icin
     * arayuzden departman da atanamiyordu — kisir dongu.
     */
    @Query("""
        SELECT e FROM Employee e
        LEFT JOIN e.department d
        WHERE e.active = true
          AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (:department IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:department AS string), '%')))
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

    /**
     * A-19 (#129): yalnizca "bu isimde aktif calisan var mi" sorusu. searchEmployees()
     * EmployeeResponse kurarken lazy department/role proxy'lerini acar ve HTTP istegi
     * disinda (IT, zamanlanmis is, LLM arac cagrisi) LazyInitializationException verir.
     * Kural katmani nesneye degil varligin kendisine baktigi icin bu sayim yeterli.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM Employee e
        WHERE e.active = true
          AND LOWER(e.name) LIKE LOWER(CONCAT('%', CAST(:token AS string), '%'))
        """)
    boolean existsActiveByNameContaining(@Param("token") String token);

    Optional<Employee> findByPhone(String phone);

    boolean existsByDepartment_Id(Integer departmentId);
}
