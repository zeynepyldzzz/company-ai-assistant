package com.company.assistant.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-6: GET /admin/schedules — tum calisanlarin haftalik duzeni (salt-okunur).
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ile korunuyor,
 * ekstra anotasyona gerek yok.
 */
@RestController
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    public AdminScheduleController(AdminScheduleService adminScheduleService) {
        this.adminScheduleService = adminScheduleService;
    }

    @GetMapping
    public ResponseEntity<AdminScheduleResponse> getAllSchedules() {
        return ResponseEntity.ok(adminScheduleService.getAllForCurrentWeek());
    }
}