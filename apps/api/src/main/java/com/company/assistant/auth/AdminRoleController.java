package com.company.assistant.auth;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// C-11 (#85): GET /admin/roles - sadece system_admin (RBAC ekrani icin rol listesi).
@RestController
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminRoleController {

    private final RoleRepository roleRepository;

    public AdminRoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }
}
