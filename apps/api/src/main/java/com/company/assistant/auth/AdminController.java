package com.company.assistant.auth;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "admin-ok");
    }

    @GetMapping("/hr-ping")
    @PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
    public Map<String, String> hrPing() {
        return Map.of("status", "hr-admin-ok");
    }
}