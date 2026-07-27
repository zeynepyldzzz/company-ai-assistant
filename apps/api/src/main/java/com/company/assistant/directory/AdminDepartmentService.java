package com.company.assistant.directory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #84 (Hafta 4): POST/PUT/DELETE /admin/departments (FR-68-71).
 */
@Service
public class AdminDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public AdminDepartmentService(DepartmentRepository departmentRepository,
                                   EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public DepartmentResponse create(AdminDepartmentRequest request) {
        Department department = new Department();
        applyRequest(department, request);
        return new DepartmentResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Integer id, AdminDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + id));
        applyRequest(department, request);
        return new DepartmentResponse(departmentRepository.save(department));
    }

    /**
     * FR-71: departman silme. employee.department_id RESTRICT ile korunuyor
     * (ON DELETE CASCADE yok); atanmis calisan varsa DB hatasi yerine anlamli
     * 409 donduruyoruz.
     */
    @Transactional
    public void delete(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadı: " + id));

        boolean calisaniVar = employeeRepository.existsByDepartment_Id(id);
        if (calisaniVar) {
            throw new DepartmentInUseException(
                    "Departmana atanmış çalışanlar olduğu için silinemez: " + id);
        }

        departmentRepository.delete(department);
    }

    private void applyRequest(Department department, AdminDepartmentRequest request) {
        department.setName(request.name());
        department.setResponsibilities(request.responsibilities());

        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new EmployeeNotFoundException(
                            "Yönetici bulunamadı: " + request.managerId()));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }
    }
}
