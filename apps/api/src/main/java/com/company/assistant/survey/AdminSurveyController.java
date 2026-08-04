package com.company.assistant.survey;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-8 (#52): Admin anket olusturma + yayimlama + sonuc goruntuleme.
 * C-14 (#123): once /admin/** yolu SecurityConfig'te sadece genel hasRole("ADMIN")
 * ile korunuyordu (hangi sub-role oldugu onemsizdi). Diger admin modulleriyle
 * (AdminVehicleController, AdminShuttleController, PolicyDocumentController vb.)
 * tutarli olmasi icin anket yonetimi hr_admin ve system_admin ile sinirlandirildi.
 */
@RestController
@RequestMapping("/admin/surveys")
@PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminSurveyController {

    private final AdminSurveyService adminSurveyService;

    public AdminSurveyController(AdminSurveyService adminSurveyService) {
        this.adminSurveyService = adminSurveyService;
    }

    /** Admin UI'da anket secip yayimlamak/sonuc gormek icin taslak+yayimlanmis TUM anketler. */
    @GetMapping
    public ResponseEntity<List<AdminSurveyResponse>> listAll() {
        return ResponseEntity.ok(adminSurveyService.listAll());
    }

    /** FR-76: yonetici anket olusturur (taslak olarak, henuz yayimlanmamis). */
    @PostMapping
    public ResponseEntity<AdminSurveyResponse> createSurvey(Authentication authentication,
                                                              @RequestBody AdminSurveyCreateRequest request) {
        Integer adminId = Integer.valueOf(authentication.getName());
        AdminSurveyResponse created = adminSurveyService.createSurvey(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Anketi yayimlar: GET /surveys/active'te gorunur, yanit kabul eder hale gelir. */
    @PutMapping("/{id}/publish")
    public ResponseEntity<AdminSurveyResponse> publish(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(adminSurveyService.publish(id));
    }

    /** C-13 (#121): anket duzenleme - baslik, secenekler, gecerlilik (deadline) tarihi. */
    @PutMapping("/{id}")
    public ResponseEntity<AdminSurveyResponse> updateSurvey(@PathVariable("id") Integer id,
                                                             @RequestBody AdminSurveyUpdateRequest request) {
        return ResponseEntity.ok(adminSurveyService.updateSurvey(id, request));
    }

    /** C-13 (#121): anket silme - bagli secenek/yanit/geri bildirim kayitlariyla birlikte. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable("id") Integer id) {
        adminSurveyService.deleteSurvey(id);
        return ResponseEntity.noContent().build();
    }

    /** FR-44: yetkili kullanicilar anket sonuclarini gorebilir. */
    @GetMapping("/{id}/results")
    public ResponseEntity<SurveyResultsResponse> getResults(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(adminSurveyService.getResults(id));
    }

    @ExceptionHandler(SurveyNotFoundException.class)
    public ResponseEntity<String> handleNotFound(SurveyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
