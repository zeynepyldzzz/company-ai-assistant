package com.company.assistant.directory;

import com.company.assistant.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DirectoryService {

    private final EmployeeRepository employeeRepository;

    public DirectoryService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public PagedResponse<EmployeeResponse> searchEmployees(
            String search, String department, String office, int page, int pageSize) {
        Page<Employee> result = employeeRepository.search(
                search, department, office, PageRequest.of(page, pageSize));
        return new PagedResponse<>(
                result.getContent().stream().map(EmployeeResponse::new).toList(),
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
        return employeeRepository.existsActiveByNameContaining(nameToken);
    }

    public EmployeeResponse getEmployeeById(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Calisan bulunamadi, id: " + id));
        return new EmployeeResponse(employee);
    }
}
