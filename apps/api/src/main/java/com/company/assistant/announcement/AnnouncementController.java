package com.company.assistant.announcement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-9 (#53): GET /announcements, GET /announcements/{id}, GET /notifications,
 * PUT /notifications/preferences.
 */
@RestController
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** FR-45-46: giris yapmis her calisan duyurulari gorebilir, sabitlenenler ustte. */
    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementDto>> getAnnouncements() {
        return ResponseEntity.ok(announcementService.getAnnouncements());
    }

    /** Ana sayfada sadece aktif (suresi dolmamis) duyurular gosterilir. */
    @GetMapping("/announcements/active")
    public ResponseEntity<List<AnnouncementDto>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementService.getActiveAnnouncements());
    }

    @GetMapping("/announcements/{id}")
    public ResponseEntity<AnnouncementDto> getAnnouncement(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(announcementService.getAnnouncement(id));
    }

    /** FR-65-67: kimlik JWT'den alinir (FR-63 pattern'i ile ayni). */
    @GetMapping("/notifications")
    public ResponseEntity<List<AnnouncementDto>> getNotifications(Authentication authentication) {
        Integer employeeId = Integer.valueOf(authentication.getName());
        return ResponseEntity.ok(announcementService.getNotifications(employeeId));
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(Authentication authentication) {
        Integer employeeId = Integer.valueOf(authentication.getName());
        return ResponseEntity.ok(announcementService.getPreferences(employeeId));
    }

    /** FR-65-67: calisan kendi bildirim tercihlerini gunceller. */
    @PutMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            Authentication authentication,
            @RequestBody NotificationPreferenceRequest request) {
        Integer employeeId = Integer.valueOf(authentication.getName());
        return ResponseEntity.ok(announcementService.updatePreferences(employeeId, request));
    }

    @ExceptionHandler(AnnouncementNotFoundException.class)
    public ResponseEntity<String> handleNotFound(AnnouncementNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
