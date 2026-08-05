package com.company.assistant.schedule;

/**
 * A-32 (#188): tek bir gunun tek bir calisan icin durumu.
 *
 * <p>{@code WeeklySchedule} ile {@code Employee} arasinda JPA iliskisi YOK — kayit
 * {@code employeeId}'yi duz {@code Integer} olarak tutuyor. Bu yuzden durum, calisan
 * sorgusuna join'lenemiyor; ayri okunup id uzerinden eslestiriliyor. Bu record o
 * eslestirmenin tasiyicisi.
 */
public record EmployeeDayStatus(Integer employeeId, ScheduleStatus status) {
}
