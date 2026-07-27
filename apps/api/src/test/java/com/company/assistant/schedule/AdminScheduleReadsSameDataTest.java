package com.company.assistant.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.company.assistant.directory.Employee;
import com.company.assistant.schedule.AdminScheduleResponse.EmployeeScheduleDto;

import static org.assertj.core.api.Assertions.assertThat;

// C-T2 (#33) Test #3: C-6 admin, calisanin kaydettigi VERININ AYNISINI okuyor.
//
// FR-64 "tek kaynak" ilkesi: C-4/C-5 (calisan tarafi, ScheduleService) ile
// C-6 (admin tarafi, AdminScheduleService) AYNI weekly_schedule/schedule_day
// tablolarindan okur; admin icin ayri bir kopya/gorunum tablosu YOKTUR.
// Bu test, calisan PUT /schedules/me ile kaydettikten SONRA admin'in
// GET /admin/schedules cevabinda tipatip ayni gunleri/gorduğunu kanitlar.
//
// @DataJpaTest sadece JPA/veritabani katmanini ayaga kaldirir; ScheduleService,
// AdminScheduleService ve ScheduleValidator normal @Service/@Component olduklari
// icin @DataJpaTest'in varsayilan component-scan'ine girmezler, o yuzden
// @Import ile elle ekliyoruz.
// @AutoConfigureTestDatabase(replace = NONE): gercek PostgreSQL'e baglan (H2 degil).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ScheduleService.class, AdminScheduleService.class, ScheduleValidator.class})
class AdminScheduleReadsSameDataTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AdminScheduleService adminScheduleService;

    @Test
    void calisaninKaydettigiHaftayi_adminAynenGorur() {
        // 1) Gercek bir calisan olustur (weekly_schedule.employee_id FK'si icin sart).
        Employee employee = new Employee();
        employee.setName("Ayni Veri Testi");
        employee.setEmail("admin-reads-same-data@example.com");
        entityManager.persistAndFlush(employee);

        // 2) Calisan tarafi: C-4/C-5 akisi ile bu haftanin duzenini kaydet.
        WeeklyScheduleDto gonderilen = new WeeklyScheduleDto(null, List.of(
                new ScheduleDayDto("monday", ScheduleStatus.OFFICE),
                new ScheduleDayDto("tuesday", ScheduleStatus.REMOTE),
                new ScheduleDayDto("wednesday", ScheduleStatus.OFFICE),
                new ScheduleDayDto("thursday", ScheduleStatus.LEAVE),
                new ScheduleDayDto("friday", ScheduleStatus.REMOTE)));
        scheduleService.saveMySchedule(employee.getId(), gonderilen);
        entityManager.flush();
        entityManager.clear();

        // 3) Admin tarafi: C-6 akisi ile TUM calisanlarin bu haftaki duzenini oku.
        AdminScheduleResponse adminCevabi = adminScheduleService.getAllForCurrentWeek();

        // 4) Ayni hafta baslangici (kopya tablo degil, ayni kaynak).
        LocalDate buHaftaninPazartesi = LocalDate.now().with(DayOfWeek.MONDAY);
        assertThat(adminCevabi.weekStartDate()).isEqualTo(buHaftaninPazartesi);

        // 5) Admin cevabinda TAM OLARAK bu calisanin kaydi bulunmali.
        Optional<EmployeeScheduleDto> calisaninKaydi = adminCevabi.employees().stream()
                .filter(e -> e.employeeId().equals(employee.getId()))
                .findFirst();
        assertThat(calisaninKaydi).isPresent();

        // 6) Gunler, calisanin PUT ile gonderdigi ile BIREBIR ayni olmali
        //    (sira ve durum dahil) — admin'in ayrica kendi kopyasi/donusumu yok.
        List<ScheduleDayDto> beklenenGunler = gonderilen.days();
        List<ScheduleDayDto> adminGunleri = calisaninKaydi.get().days();
        assertThat(adminGunleri).containsExactlyElementsOf(beklenenGunler);
    }
}
