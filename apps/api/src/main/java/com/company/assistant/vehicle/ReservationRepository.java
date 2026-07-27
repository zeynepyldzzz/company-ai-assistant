package com.company.assistant.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    List<Reservation> findByEmployeeIdOrderByStartTimeDesc(Integer employeeId);

    Optional<Reservation> findByIdAndEmployeeId(Integer id, Integer employeeId);

    // Cakisma kontrolu (B-8/B-T3 kritik kural): iptal edilmemis ve zaman araligi kesisen
    // rezervasyonlari bulur. start_time < :endTime AND end_time > :startTime -> klasik araliktan
    // kesisme kosulu.
    List<Reservation> findByVehicleIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Integer vehicleId, ReservationStatus status, LocalDateTime endTime, LocalDateTime startTime);
}
