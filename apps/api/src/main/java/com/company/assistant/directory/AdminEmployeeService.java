package com.company.assistant.directory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.auth.Role;
import com.company.assistant.auth.RoleRepository;

/**
 * #84 (Hafta 4): POST/PUT/DELETE /admin/employees (FR-68-71).
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ile korunuyor; controller
 * seviyesinde hr_admin/system_admin'e daraltilir (PolicyDocumentController deseni).
 */
@Service
public class AdminEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public AdminEmployeeService(EmployeeRepository employeeRepository,
                                 DepartmentRepository departmentRepository,
                                 RoleRepository roleRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public EmployeeResponse create(AdminEmployeeRequest request) {
        Employee employee = new Employee();
        applyRequest(employee, request);
        employee.setActive(true);
        return new EmployeeResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Integer id, AdminEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Çalışan bulunamadı: " + id));
        applyRequest(employee, request);
        return new EmployeeResponse(employeeRepository.save(employee));
    }

    /**
     * FR-71: calisan silme. Employee birçok tabloda (haftalik duzen, anket yaniti,
     * arac rezervasyonu vb.) referans edildigi icin hard delete FK ihlaline yol
     * acar; is_active=false ile "soft delete" yapiliyor (kolon zaten bu amacla var).
     */
    @Transactional
    public void delete(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Çalışan bulunamadı: " + id));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private void applyRequest(Employee employee, AdminEmployeeRequest request) {
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setOfficeStatus(request.officeStatus());

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException(
                            "Departman bulunamadı: " + request.departmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + request.roleId()));
            employee.setRole(role);
        }
    }
}
