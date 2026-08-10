package com.company.assistant.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-6: GET /admin/schedules — tum calisanlarin haftalik duzeni (salt-okunur).
 *
 * <p>A-36 (#200): rol daraltmasi EKLENDI. Onceden yalnizca SecurityConfig'in
 * {@code /admin/**} kurali koruyordu, yani her admin alt rolu (canteen_admin,
 * shuttle_admin dahil) tum calisanlarin haftalik planini gorebiliyordu. Bu, calisan
 * verisi; yemekhane ya da servis yoneticisinin kisi kisi plan gormesi icin islevsel bir
 * gerekce yok. Diger sekiz yonetim ucunun hepsinde rol daraltmasi var, bu tek istisnaydi.
 *
 * <p>Servis planlamasi icin "kac kisi ofiste olacak" bilgisi gerekirse dogru cozum
 * servis tarafina ozet sayi vermek; kisi listesi acmak degil.
 */
@RestController
@RequestMapping("/admin/schedules")
@PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
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