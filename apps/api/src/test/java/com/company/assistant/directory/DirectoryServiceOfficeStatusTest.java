package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.schedule.ScheduleStatus;
import com.company.assistant.schedule.TodayStatusService;

/**
 * A-32 (#188): ofis durumunun BUGUNUN calisma duzeninden gelmesi.
 *
 * <p>Onceden {@code employee.office_status} kolonundan geliyordu; kolon duragandi ve elle set
 * ediliyordu, hicbir sey onu planla senkron tutmuyordu. Olculdu: plani olan tek calisanda
 * kolon "Ofiste", plan "REMOTE" diyordu.
 */
@ExtendWith(MockitoExtension.class)
class DirectoryServiceOfficeStatusTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TodayStatusService todayStatusService;

    private DirectoryService service;

    @BeforeEach
    void setUp() {
        service = new DirectoryService(employeeRepository, todayStatusService);
    }

    private Employee employee(int id, String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        // Kolon bilerek DOLU birakiliyor: testin amaci, dolu olsa bile okunmadigini gostermek.
        employee.setOfficeStatus("Ofiste");
        return employee;
    }

    private void stubSearch(Employee... employees) {
        Page<Employee> page = new PageImpl<>(List.of(employees));
        when(employeeRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(todayStatusService.currentWeekStart()).thenReturn(LocalDate.of(2026, 8, 3));
        when(todayStatusService.todayKey()).thenReturn("wednesday");
    }

    @Test
    void durumPlandanGelir_kolondanDegil() {
        stubSearch(employee(5, "Elif", "Sahin"));
        when(todayStatusService.statusesForToday()).thenReturn(Map.of(5, ScheduleStatus.REMOTE));

        PagedResponse<EmployeeResponse> result = service.searchEmployees(null, null, null, 0, 20);

        // Kolon "Ofiste" diyor, plan "REMOTE" — plan kazanmali.
        assertThat(result.data().get(0).getOfficeStatus()).isEqualTo("Uzaktan");
    }

    // Plani olmayan calisan icin office_status'a GERI DUSULMEZ. Fallback iki kaynagi
    // yasatmaya devam ederdi ve celiskinin sebebi tam olarak oydu.
    @Test
    void planYoksaDurumBostur_kolonaGeriDusulmez() {
        stubSearch(employee(7, "Can", "Ozturk"));
        when(todayStatusService.statusesForToday()).thenReturn(Map.of());

        PagedResponse<EmployeeResponse> result = service.searchEmployees(null, null, null, 0, 20);

        assertThat(result.data().get(0).getOfficeStatus()).isNull();
    }

    // Rehber etiketi plan enum'una cevrilip sorguya oyle gidiyor; ceviri kayarsa filtre
    // sessizce hicbir seyle eslesmez.
    @Test
    void rehberEtiketiPlanDurumunaCevrilir() {
        stubSearch(employee(5, "Elif", "Sahin"));
        when(todayStatusService.statusesForToday()).thenReturn(Map.of());

        service.searchEmployees(null, null, "Izinde", 0, 20);

        verify(employeeRepository).search(
                isNull(), isNull(), eq(ScheduleStatus.LEAVE), any(), any(), any(Pageable.class));
    }

    /**
     * Taninmayan bir durum degeri (orn. elle yazilmis {@code ?office=asdf}) TUM listeyi
     * dondurmemeli. Ceviri null dondugunde filtre sessizce dusseydi, kullanici filtre
     * uygulandigini sanip filtrelenmemis liste gorurdu.
     */
    @Test
    void taninmayanDurumFiltresiBosDoner_sorguHicCalismaz() {
        PagedResponse<EmployeeResponse> result = service.searchEmployees(null, null, "asdf", 0, 20);

        assertThat(result.data()).isEmpty();
        assertThat(result.total()).isZero();
        verify(employeeRepository, never())
                .search(any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
