package com.company.assistant.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;
import com.company.assistant.schedule.AdminScheduleResponse.EmployeeScheduleDto;

/**
 * C-6: Tum calisanlarin haftalik duzenini salt-okunur getirir.
 * FR-64: C-4'un weekly_schedule tablosundan okur, kopya olusturmaz.
 */
@Service
public class AdminScheduleService {

    private static final List<String> DAY_ORDER =
            List.of("monday", "tuesday", "wednesday", "thursday", "friday");

    private final WeeklyScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;

    public AdminScheduleService(WeeklyScheduleRepository scheduleRepository,
                                 EmployeeRepository employeeRepository) {
        this.scheduleRepository = scheduleRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public AdminScheduleResponse getAllForCurrentWeek() {
        LocalDate weekStart = ScheduleService.currentWeekStart();

        // 1) Bu haftanin tum kayitlari (gunleriyle birlikte)
        List<WeeklySchedule> schedules = scheduleRepository.findAllByWeekWithDays(weekStart);

        // 2) Calisan adlarini tek sorguda topla: employeeId -> name
        List<Integer> employeeIds = schedules.stream()
                .map(WeeklySchedule::getEmployeeId)
                .toList();
        Map<Integer, String> nameById = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));

        // 3) Birlestir: her calisan icin gunleri sabit sirayla diz
        List<EmployeeScheduleDto> employees = schedules.stream()
                .map(ws -> new EmployeeScheduleDto(
                        ws.getEmployeeId(),
                        nameById.getOrDefault(ws.getEmployeeId(), "Bilinmeyen"),
                        toOrderedDays(ws)))
                .sorted((a, b) -> a.employeeName().compareToIgnoreCase(b.employeeName()))
                .toList();

        return new AdminScheduleResponse(weekStart, employees);
    }

    /** Gunleri Pazartesi-Cuma sirasinda DTO'ya cevirir. */
    private List<ScheduleDayDto> toOrderedDays(WeeklySchedule schedule) {
        Map<String, ScheduleDay> byDay = schedule.getDays().stream()
                .collect(Collectors.toMap(
                        d -> d.getDayOfWeek().toLowerCase(),
                        Function.identity()));
        return DAY_ORDER.stream()
                .filter(byDay::containsKey)
                .map(day -> new ScheduleDayDto(day, byDay.get(day).getStatus()))
                .toList();
    }
}