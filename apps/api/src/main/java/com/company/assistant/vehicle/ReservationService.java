package com.company.assistant.vehicle;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;

    public ReservationService(ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository, EmployeeRepository employeeRepository) {
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
    }

    // FR-39: rezervasyon olusturma; B-T3 kritik kural: cakisan zaman araligi reddedilir.
    @Transactional
    public ReservationResponse createReservation(Integer employeeId, ReservationRequest request) {
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Başlangıç zamanı geçmişte olamaz");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitiş zamanı başlangıç zamanından sonra olmalıdır");
        }

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException("Araç bulunamadı, id: " + request.vehicleId()));

        if (vehicle.getMaintenanceStatus() != MaintenanceStatus.AVAILABLE) {
            throw new ReservationConflictException("Bu araç şu anda bakımda, rezervasyon yapılamaz");
        }

        List<Reservation> overlapping = reservationRepository
                .findByVehicleIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        request.vehicleId(), ReservationStatus.CANCELLED, request.endTime(), request.startTime());
        if (!overlapping.isEmpty()) {
            throw new ReservationConflictException(
                    "Bu araç seçilen zaman aralığında zaten rezerve edilmiş");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Çalışan bulunamadı"));

        Reservation reservation = new Reservation();
        reservation.setVehicle(vehicle);
        reservation.setEmployee(employee);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        return new ReservationResponse(reservation);
    }

    // FR-40: kullanicinin kendi rezervasyonlari.
    public List<ReservationResponse> listMyReservations(Integer employeeId) {
        return reservationRepository.findByEmployeeIdOrderByStartTimeDesc(employeeId)
                .stream().map(ReservationResponse::new).toList();
    }

    // FR-39: kullanici yalnizca kendi rezervasyonunu iptal edebilir.
    @Transactional
    public void cancelReservation(Integer employeeId, Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdAndEmployeeId(reservationId, employeeId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Rezervasyon bulunamadı, id: " + reservationId));
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
}
