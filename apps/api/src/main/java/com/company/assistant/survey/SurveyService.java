package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-7 (#51): GET /surveys/active, POST /surveys/{id}/responses, POST /feedback.
 */
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final FeedbackRepository feedbackRepository;

    public SurveyService(SurveyRepository surveyRepository,
                          SurveyResponseRepository surveyResponseRepository,
                          FeedbackRepository feedbackRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** GET /surveys/active — FR-42. Semada durum kolonu yok, tum anketler aktif sayilir. */
    @Transactional(readOnly = true)
    public List<SurveyDto> getActiveSurveys() {
        return surveyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SurveyDto::from)
                .toList();
    }

    /**
     * POST /surveys/{id}/responses — FR-42.
     * FR-63'teki pattern ile ayni: kimlik JWT'den (authentication.getName()) gelir,
     * govdeden degil; o yuzden employeeId parametre olarak controller'dan aliniyor.
     */
    @Transactional
    public void submitResponse(Integer surveyId, Integer employeeId, SurveyResponseRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));

        SurveyResponse response = new SurveyResponse();
        response.setSurvey(survey);
        response.setEmployeeId(employeeId);
        response.setAnswers(request == null ? null : request.answers());
        surveyResponseRepository.save(response);
    }

    /**
     * POST /feedback — FR-43: anonim geri bildirim.
     * BILINCLI OLARAK employeeId ALINMAZ/KAYDEDILMEZ — anonimlik sema
     * seviyesinde (feedback tablosunda employee_id kolonu yok) garanti edilir.
     */
    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        Feedback feedback = new Feedback();
        if (request != null && request.surveyId() != null) {
            Survey survey = surveyRepository.findById(request.surveyId())
                    .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + request.surveyId()));
            feedback.setSurvey(survey);
        }
        feedback.setContent(request == null ? null : request.content());
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }
}
