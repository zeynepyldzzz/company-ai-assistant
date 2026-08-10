package com.company.assistant.schedule;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.hibernate.exception.ConstraintViolationException;

import com.company.assistant.directory.Employee;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// C-T2 (#33) Test #2: FR-64 tek kaynak — (employeeId, weekStartDate) unique kisiti.
// Ayni calisan + ayni hafta icin IKI kayit yazilamaz; veritabani ikincisini reddeder.
//
// @DataJpaTest sadece JPA/veritabani katmanini ayaga kaldirir (tum uygulamayi degil).
// @AutoConfigureTestDatabase(replace = NONE): sahte (H2) veritabani KULLANMA,
//   application.yml'deki gercek PostgreSQL'e baglan (pgvector/pg_trgm uyumu icin sart).
// Her test sonunda @DataJpaTest otomatik geri sarar (rollback) — gercek veri kirlenmez.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WeeklyScheduleConstraintTest {

    @Autowired
    private TestEntityManager entityManager;

    private WeeklySchedule schedule(Integer employeeId, LocalDate weekStart) {
        WeeklySchedule ws = new WeeklySchedule();
        ws.setEmployeeId(employeeId);
        ws.setWeekStartDate(weekStart);
        return ws;
    }

    private Employee calisan(String email) {
        Employee e = new Employee();
        e.setFirstName("Test");
        e.setLastName("Calisan");
        e.setEmail(email);
        return e;
    }

    @Test
    void ayniCalisanAyniHafta_ikinciKayitReddedilir() {
        LocalDate hafta = LocalDate.of(2026, 7, 20);

        // 0) weekly_schedule.employee_id -> employee(id) FK kisiti var;
        //    once gercek bir employee kaydi olusturup uretilen id'yi kullanmaliyiz.
        //    persistAndFlush void doner; IDENTITY strateji sayesinde id, persist
        //    sirasinda ayni nesne uzerinde doldurulur.
        Employee employee = calisan("weekly-schedule-test+" + hafta + "@example.com");
        entityManager.persistAndFlush(employee);

        // 1) Ilk kayit: sorunsuz yazilmali.
        entityManager.persistAndFlush(schedule(employee.getId(), hafta));

        // 2) Ayni employeeId + ayni weekStartDate ile ikinci kayit.
        WeeklySchedule kopya = schedule(employee.getId(), hafta);

        // 3) Yazmayi denedigimizde veritabani unique kisiti ihlalinden dolayi patlamali.
        //    persistAndFlush -> "hemen diske yaz", boylece hata tam burada firlar.
        //    NOT: TestEntityManager, Spring'in @Repository proxy katmanini atlar,
        //    bu yuzden Spring'in DataIntegrityViolationException'a cevirmesi devreye
        //    girmez; ham Hibernate/JDBC istisnasi (ConstraintViolationException) firlar.
        assertThatThrownBy(() -> entityManager.persistAndFlush(kopya))
                .isInstanceOf(ConstraintViolationException.class);
    }
}