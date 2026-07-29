package com.company.assistant.directory;

import com.company.assistant.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public PagedResponse<DepartmentResponse> searchDepartments(String search, int page, int pageSize) {
        Page<Department> result = departmentRepository.search(search, PageRequest.of(page, pageSize));
        return new PagedResponse<>(
                result.getContent().stream().map(DepartmentResponse::new).toList(),
                page,
                pageSize,
                result.getTotalElements()
        );
    }

    /**
     * A-19 (#129): yalnizca departman adlari. searchDepartments() yonetici bilgisini de
     * kurdugu icin lazy proxy acar ve HTTP istegi disinda calismaz; kural katmani yalnizca
     * ada baktigi icin bu projeksiyon hem dogru hem ucuz.
     */
    public List<String> getDepartmentNames() {
        return departmentRepository.findAllNames();
    }

    public DepartmentResponse getDepartmentById(Integer id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Departman bulunamadi, id: " + id));
        return new DepartmentResponse(department);
    }
}
