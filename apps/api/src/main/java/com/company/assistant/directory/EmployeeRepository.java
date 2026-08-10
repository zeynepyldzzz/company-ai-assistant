package com.company.assistant.directory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

import com.company.assistant.schedule.ScheduleStatus;

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
     *
     * <p>A-32 (#188): ofis durumu filtresi artik {@code employee.office_status} kolonuna DEGIL,
     * BUGUNUN {@code schedule_day} kaydina bakiyor. Kolon duragan ve elle set ediliyordu; plan
     * ile celisebiliyordu (olculdu: plani olan tek calisanda kolon "Ofiste", plan "REMOTE").
     *
     * <p>Filtre neden alt sorgu: {@code WeeklySchedule} ile {@code Employee} arasinda JPA
     * iliskisi yok ({@code employeeId} duz Integer). {@code EXISTS} yazimi hem iliskisiz
     * eslestirmeyi cozuyor hem de sayfalamayi bozmuyor — filtre SQL'de uygulandigi icin
     * {@code total} dogru kaliyor. Durumu Java'da filtreleseydik sayfa boyutu ve toplam
     * sayilar tutarsiz olurdu.
     *
     * <p>Plani olmayan calisan hicbir durum filtresine dusmez; filtresiz listede ise gorunur
     * ve durumu "girilmedi" olarak doner.
     *
     * <p>A-34 (#194): isim aramasi KELIME BASI eslesmesi. Onceden iki taraf da joker'di
     * ({@code %a%}) ve "a" yazildiginda isminin herhangi bir yerinde a gecen herkes
     * doniyordu — 15 kisilik bir sirkette pratikte tum liste; arama kutusu filtrelemiyordu.
     * Yalnizca bastan eslesme ({@code a%}) de yeterli degil, soyadiyla arama kirilirdi
     * ("kay" -> Ayse Kaya bulunamazdi) ve rehberde en cok yapilan sey o.
     *
     * <p>A-35 (#196): ad ve soyad ayri kolonlar oldugu icin "kelime basi" artik taklit
     * edilmiyor. A-34'te tek {@code name} kolonu uzerinde {@code LIKE '% ' || :s || '%'}
     * yazmak zorundaydik — bosluk karakteriyle kelime siniri aramak. Simdi iki alana ayri
     * ayri prefix eslesmesi yetiyor: hem daha okunur hem de cift bosluk, tire gibi
     * ayiricilarda sessizce kaymiyor.
     *
     * <p>{@code :department} filtresi BILEREK joker kaldi: degeri secim kutusundan tam ad
     * olarak geliyor, kullanicinin yazdigi serbest metin degil.
     *
     * <p>A-34: <b>ORDER BY sart.</b> Onceden ne sorguda ne {@code PageRequest}'te siralama
     * vardi. Postgres sirasiz bir sorguda satir sirasini garanti etmez; {@code LIMIT/OFFSET}
     * ile birlikte ayni kisi iki sayfada birden cikabilir, baskasi hic gorunmeyebilir.
     * Siralama cagiran tarafa birakilirsa biri {@code PageRequest.of(page, size)} yazip
     * sessizce bozar, o yuzden sorguda.
     *
     * <p>A-35: siralama artik SOYADA gore — rehberlerin klasik davranisi. {@code lastName}
     * NULL olan eski kayitlar (tek kelimeli test hesaplari) Postgres'in varsayilaniyla
     * sona duser.
     */
    @Query("""
        SELECT e FROM Employee e
        LEFT JOIN e.department d
        WHERE e.active = true
          AND (:search IS NULL
               OR LOWER(e.firstName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT(CAST(:search AS string), '%')))
          AND (:department IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:department AS string), '%')))
          AND (:office IS NULL OR EXISTS (
                SELECT 1 FROM WeeklySchedule ws
                JOIN ws.days sd
                WHERE ws.employeeId = e.id
                  AND ws.weekStartDate = :weekStart
                  AND LOWER(sd.dayOfWeek) = :dayOfWeek
                  AND sd.status = :office))
        ORDER BY e.lastName, e.firstName, e.id
        """)
    Page<Employee> search(
            @Param("search") String search,
            @Param("department") String department,
            @Param("office") ScheduleStatus office,
            @Param("weekStart") LocalDate weekStart,
            @Param("dayOfWeek") String dayOfWeek,
            Pageable pageable
    );

    /**
     * A-34 (#194): isim tarafi kelime basi eslesmesi (bkz. {@link #search}).
     *
     * <p>Telefon tarafi BILEREK substring kaldi: kullanici numaranin son hanelerinden arama
     * yapabilmeli ve bir telefon numarasinda "kelime basi" diye bir sey yok.
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE e.active = true
          AND e.phone IS NOT NULL
          AND (:search IS NULL
               OR LOWER(e.firstName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT(CAST(:search AS string), '%'))
               OR e.phone LIKE CONCAT('%', CAST(:search AS string), '%'))
        ORDER BY e.lastName, e.firstName, e.id
        """)
    Page<Employee> searchPhonebook(@Param("search") String search, Pageable pageable);

    /**
     * A-19 (#129): yalnizca "bu isimde aktif calisan var mi" sorusu. searchEmployees()
     * EmployeeResponse kurarken lazy department/role proxy'lerini acar ve HTTP istegi
     * disinda (IT, zamanlanmis is, LLM arac cagrisi) LazyInitializationException verir.
     * Kural katmani nesneye degil varligin kendisine baktigi icin bu sayim yeterli.
     *
     * <p>A-34 (#194): kelime basi eslesmesi — metot adi da bu yuzden degisti. Substring
     * eslesmesi burada yalnizca kozmetik degildi: mesajda gecen "ali" kelimesi, sirkette
     * "Kemali" adinda biri varsa kural katmanini YANLIS tetikliyordu.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM Employee e
        WHERE e.active = true
          AND (LOWER(e.firstName) LIKE LOWER(CONCAT(CAST(:token AS string), '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT(CAST(:token AS string), '%')))
        """)
    boolean existsActiveByNameWordPrefix(@Param("token") String token);

    Optional<Employee> findByPhone(String phone);

    boolean existsByDepartment_Id(Integer departmentId);
}
