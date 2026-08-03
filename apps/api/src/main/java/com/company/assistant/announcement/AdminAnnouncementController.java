package com.company.assistant.announcement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-9 (#53): POST /admin/announcements, PUT /admin/announcements/{id}/pin.
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ile korunuyor.
 */
@RestController
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    public AdminAnnouncementController(AdminAnnouncementService adminAnnouncementService) {
        this.adminAnnouncementService = adminAnnouncementService;
    }

    /** FR-46-47: yonetici duyuru olusturur. */
    @PostMapping
    public ResponseEntity<AnnouncementDto> create(Authentication authentication,
                                                    @RequestBody AdminAnnouncementCreateRequest request) {
        Integer adminId = Integer.valueOf(authentication.getName());
        AnnouncementDto created = adminAnnouncementService.create(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** FR-46-47: duyuruyu sabitler/sabitlemeyi kaldirir (toggle). */
    @PutMapping("/{id}/pin")
    public ResponseEntity<AnnouncementDto> togglePin(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(adminAnnouncementService.togglePin(id));
    }

    /** Duyuru duzenleme: baslik, icerik ve gecerlilik tarihi guncellenir. */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementDto> update(@PathVariable("id") Integer id,
                                                    @RequestBody AdminAnnouncementUpdateRequest request) {
        return ResponseEntity.ok(adminAnnouncementService.update(id, request));
    }

    @ExceptionHandler(AnnouncementNotFoundException.class)
    public ResponseEntity<String> handleNotFound(AnnouncementNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
