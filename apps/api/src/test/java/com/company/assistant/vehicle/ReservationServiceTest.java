package com.company.assistant.vehicle;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// B-T3: Rezervasyon cakisma engeli (FR-38-41 kritik kural) + kullanicinin kendi
// rezervasyonlarini goruntuleyip iptal edebilmesi (FR-39, 40).
class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private VehicleRepository vehicleRepository;
    private EmployeeRepository employeeRepository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new ReservationService(reservationRepository, vehicleRepository, employeeRepository);
    }

    private Vehicle vehicle(Integer id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setPlate("34 ABC 123");
        vehicle.setMaintenanceStatus(MaintenanceStatus.AVAILABLE);
        return vehicle;
    }

    private Employee employee(Integer id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFirstName("Test");
        employee.setLastName("Calisan");
        return employee;
    }

    @Test
    void cakisanZamanAraligindaRezervasyonReddedilir() {
        Integer vehicleId = 1;
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(vehicleId, start, end);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));

        Reservation existing = new Reservation();
        existing.setId(5);
        existing.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findByVehicleIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(vehicleId), eq(ReservationStatus.CANCELLED), eq(end), eq(start)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createReservation(42, request))
                .isInstanceOf(ReservationConflictException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cakismayanRezervasyonOnaylanir() {
        Integer vehicleId = 1;
        Integer employeeId = 42;
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(vehicleId, start, end);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle(vehicleId)));
        when(reservationRepository.findByVehicleIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(vehicleId), eq(ReservationStatus.CANCELLED), eq(end), eq(start)))
                .thenReturn(List.of());
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));

        ReservationResponse response = service.createReservation(employeeId, request);

        assertThat(response.getVehicleId()).isEqualTo(vehicleId);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void bitisZamaniBaslangictanOnceyseHataFirlatir() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.minusHours(2);
        ReservationRequest request = new ReservationRequest(1, start, end);

        assertThatThrownBy(() -> service.createReservation(42, request))
                .isInstanceOf(ResponseStatusException.class);

        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void gecmisBirBaslangicZamaniIcinRezervasyonReddedilir() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(1, start, end);

        assertThatThrownBy(() -> service.createReservation(42, request))
                .isInstanceOf(ResponseStatusException.class);

        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void bakimdakiAracIcinRezervasyonReddedilir() {
        Integer vehicleId = 1;
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(vehicleId, start, end);

        Vehicle maintenanceVehicle = vehicle(vehicleId);
        maintenanceVehicle.setMaintenanceStatus(MaintenanceStatus.MAINTENANCE);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(maintenanceVehicle));

        assertThatThrownBy(() -> service.createReservation(42, request))
                .isInstanceOf(ReservationConflictException.class);

        verify(reservationRepository, never())
                .findByVehicleIdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(any(), any(), any(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void olmayanAracIcinRezervasyonYapilamaz() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        ReservationRequest request = new ReservationRequest(999, start, end);

        when(vehicleRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createReservation(42, request))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void kullaniciKendiRezervasyonunuIptalEdebilir() {
        Reservation reservation = new Reservation();
        reservation.setId(7);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findByIdAndEmployeeId(7, 42)).thenReturn(Optional.of(reservation));

        service.cancelReservation(42, 7);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void baskasininRezervasyonunuIptalEdemez() {
        when(reservationRepository.findByIdAndEmployeeId(7, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelReservation(99, 7))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
