package com.company.assistant.survey;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C-7 (#51): GET /surveys/active, POST /surveys/{id}/responses, POST /feedback.
 */
@RestController
public class SurveyController {

    private final SurveyService surveyService;

    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping("/surveys/ping")
    public String ping() {
        return "survey module OK";
    }

    /**
     * FR-42: giris yapmis her calisan aktif anketleri gorebilir.
     *
     * <p>A-33 (#192): yanittaki {@code answered} alani ISTEGI YAPAN calisana gore doluyor,
     * bu yuzden kimlik JWT'den okunuyor (submitResponse ile ayni kural).
     */
    @GetMapping("/surveys/active")
    public ResponseEntity<List<SurveyDto>> getActiveSurveys(Authentication authentication) {
        Integer employeeId = Integer.valueOf(authentication.getName());
        return ResponseEntity.ok(surveyService.getActiveSurveys(employeeId));
    }

    /**
     * FR-42: kimlik her zaman JWT'den alinir (ScheduleController'daki FR-63
     * pattern'i ile ayni), istek govdesinden veya URL'den DEGIL.
     */
    @PostMapping("/surveys/{id}/responses")
    public ResponseEntity<Void> submitResponse(@PathVariable("id") Integer surveyId,
                                                Authentication authentication,
                                                @RequestBody SurveyResponseRequest request) {
        Integer employeeId = Integer.valueOf(authentication.getName());
        surveyService.submitResponse(surveyId, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * C-13 (#121): calisana acik response-count endpoint'i (dashboard progress bar icin).
     * Admin'e ozel /admin/surveys/{id}/results'tan farkli olarak sonuc detayi vermez,
     * sadece toplam yanit sayisini dondurur.
     */
    @GetMapping("/surveys/{id}/response-count")
    public ResponseEntity<SurveyResponseCountResponse> getResponseCount(@PathVariable("id") Integer surveyId) {
        return ResponseEntity.ok(surveyService.getResponseCount(surveyId));
    }

    /**
     * FR-43: anonim geri bildirim. Authentication PARAMETRE OLARAK BILE ALINMAZ —
     * employeeId hicbir sekilde okunmaz/kaydedilmez (anonimlik garantisi).
     */
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest request) {
        surveyService.submitFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ExceptionHandler(SurveyNotFoundException.class)
    public ResponseEntity<String> handleNotFound(SurveyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(SurveyNotPublishedException.class)
    public ResponseEntity<String> handleNotPublished(SurveyNotPublishedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(SurveyAlreadyRespondedException.class)
    public ResponseEntity<String> handleAlreadyResponded(SurveyAlreadyRespondedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(SurveyDeadlinePassedException.class)
    public ResponseEntity<String> handleDeadlinePassed(SurveyDeadlinePassedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
