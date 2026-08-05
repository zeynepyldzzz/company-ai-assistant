package com.company.assistant.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A-32 (#188): "bugun kim nerede" sorusunun TEK sahibi.
 *
 * <p>Bu sinif oncesinde iki ayri gerceklik vardi: {@code employee.office_status} elle set
 * edilen duragan bir kolondu, {@code weekly_schedule}/{@code schedule_day} ise gercek plani
 * tutuyordu. Kimse ikisini senkronlamiyordu — olculdu: planı olan tek calisanda rehber
 * "Ofiste", plan "REMOTE" diyordu. Rehber ve chatbot artik yalnizca buradan besleniyor.
 *
 * <p><b>Plan yoksa durum da yoktur.</b> {@code office_status}'a geri dusulmez: fallback iki
 * kaynagi yasatmaya devam ederdi ve celiskinin sebebi tam olarak oydu. Cagiran taraf bos
 * sonucu "plan girilmedi" olarak gosterir — bilinmeyeni bilgiymis gibi sunmaktansa
 * bilinmedigini soylemek dogru.
 *
 * <p>Faz 2 notu: LLM arac cagrisi da bu servisi kullanabilir; disariya HTTP'ye ya da
 * Authentication'a bagli bir sey sizmiyor.
 */
@Service
public class TodayStatusService {

    private final WeeklyScheduleRepository repository;

    public TodayStatusService(WeeklyScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * Bugun plani girili olan calisanlarin durumu.
     *
     * <p>Hafta sonunda BOS map doner — {@code schedule_day} yalnizca Pazartesi-Cuma tutuyor
     * (FR-60), dolayisiyla Cumartesi/Pazar icin "veri yok" ile "plan girilmemis" ayni sey
     * degil. Ayrimi cagiran taraf {@link #isWorkday()} ile yapar.
     */
    @Transactional(readOnly = true)
    public Map<Integer, ScheduleStatus> statusesForToday() {
        if (!isWorkday()) {
            return Map.of();
        }
        List<EmployeeDayStatus> rows = repository.findStatusesByDay(
                ScheduleService.currentWeekStart(), todayKey());

        // Ayni (schedule, gun) icin unique kisit var (V16), dolayisiyla anahtar cakismasi
        // beklenmiyor; yine de merge fonksiyonu veriliyor ki bozuk veri exception yerine
        // ilk kaydi kullansin — rehber listesi tek bir bozuk satir yuzunden dusmemeli.
        return rows.stream().collect(Collectors.toMap(
                EmployeeDayStatus::employeeId,
                EmployeeDayStatus::status,
                (first, duplicate) -> first));
    }

    /** Bugun plan tutulan bir gun mu (Pazartesi-Cuma). */
    public boolean isWorkday() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY;
    }

    /**
     * Icinde bulundugumuz haftanin Pazartesi'si.
     *
     * <p>Rehber sorgusu durumu SQL'de filtreledigi icin (sayfalama bozulmasin diye) bu iki
     * degeri disari vermek zorundayiz; hesabin iki yerde tekrarlanmamasi icin kaynak burasi.
     */
    public LocalDate currentWeekStart() {
        return ScheduleService.currentWeekStart();
    }

    /** {@code schedule_day.day_of_week} degerleriyle ayni bicim: kucuk harf Ingilizce gun adi. */
    public String todayKey() {
        return LocalDate.now().getDayOfWeek().name().toLowerCase(Locale.ROOT);
    }
}
