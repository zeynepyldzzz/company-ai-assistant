package com.company.assistant.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /** Modul saglik kontrolu (iskeletten korundu). */
    @GetMapping("/ping")
    public String ping() {
        return "schedule module OK";
    }

    /** FR-63: kimlik her zaman JWT'den alinir, URL'den degil. */
    private Integer currentEmployeeId(Authentication authentication) {
        return Integer.valueOf(authentication.getName());
    }

    @GetMapping("/me")
    public ResponseEntity<WeeklyScheduleDto> getMySchedule(Authentication authentication) {
        return ResponseEntity.ok(scheduleService.getMySchedule(currentEmployeeId(authentication)));
    }

    @PutMapping("/me")
    public ResponseEntity<WeeklyScheduleDto> saveMySchedule(Authentication authentication,
                                                            @RequestBody WeeklyScheduleDto body) {
        return ResponseEntity.ok(scheduleService.saveMySchedule(currentEmployeeId(authentication), body));
    }

    @GetMapping("/me/summary")
    public ResponseEntity<ScheduleSummaryDto> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(scheduleService.getMySummary(currentEmployeeId(authentication)));
    }
}