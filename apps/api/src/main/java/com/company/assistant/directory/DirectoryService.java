package com.company.assistant.directory;

import java.util.List;
import java.util.Map;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.schedule.ScheduleStatus;
import com.company.assistant.schedule.TodayStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DirectoryService {

    private final EmployeeRepository employeeRepository;
    private final TodayStatusService todayStatusService;

    public DirectoryService(EmployeeRepository employeeRepository,
                            TodayStatusService todayStatusService) {
        this.employeeRepository = employeeRepository;
        this.todayStatusService = todayStatusService;
    }

    /**
     * A-32 (#188): ofis durumu artik {@code employee.office_status} kolonundan degil, BUGUNUN
     * {@code schedule_day} kaydindan geliyor. Kolon duragandi ve elle set ediliyordu; hicbir
     * sey onu planla senkron tutmuyordu.
     *
     * <p>Iki sorgu calisiyor: biri (filtreli, sayfali) calisanlari, digeri bugunun durum
     * haritasini getiriyor. Durumu ikinci sorgudan eslestirmek zorundayiz cunku
     * {@code WeeklySchedule} ile {@code Employee} arasinda JPA iliskisi yok. Harita bir
     * gunluk tum kayitlari icerir; sayfa basina degil istek basina bir kez okunur.
     */
    public PagedResponse<EmployeeResponse> searchEmployees(
            String search, String department, String office, int page, int pageSize) {

        ScheduleStatus officeFilter = OfficeStatusLabels.statusFor(office);
        // Taninmayan bir durum degeri (orn. elle yazilmis ?office=asdf) TUM listeyi
        // dondurmemeli — filtre uygulanmis gibi gorunup uygulanmamis olur. Eski davranis da
        // bostu (kolon esitligi tutmuyordu), korunuyor.
        if (office != null && officeFilter == null) {
            return new PagedResponse<>(List.of(), page, pageSize, 0);
        }

        Page<Employee> result = employeeRepository.search(
                search, department, officeFilter,
                todayStatusService.currentWeekStart(), todayStatusService.todayKey(),
                PageRequest.of(page, pageSize));

        Map<Integer, ScheduleStatus> todayStatuses = todayStatusService.statusesForToday();
        return new PagedResponse<>(
                result.getContent().stream()
                        .map(employee -> new EmployeeResponse(
                                employee, todayStatuses.get(employee.getId())))
                        .toList(),
                page,
                pageSize,
                result.getTotalElements()
        );
    }

    /**
     * A-19 (#129): kural katmani icin varlik kontrolu. searchEmployees() EmployeeResponse
     * kurdugu icin lazy department/role proxy'lerini acar ve HTTP istegi disinda calismaz;
     * burada yalnizca "bu isimde aktif calisan var mi" sorusu var.
     */
    public boolean existsActiveEmployeeNamed(String nameToken) {
        return employeeRepository.existsActiveByNameWordPrefix(nameToken);
    }

    public EmployeeResponse getEmployeeById(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Calisan bulunamadi, id: " + id));
        return new EmployeeResponse(employee, todayStatusService.statusesForToday().get(id));
    }
}
