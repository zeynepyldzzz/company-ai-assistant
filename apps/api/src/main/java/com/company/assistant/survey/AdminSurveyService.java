package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-8 (#52): POST /admin/surveys, PUT /admin/surveys/{id}/publish, GET /admin/surveys/{id}/results.
 * FR-44/FR-76: yetkili (ADMIN) kullanicilar anket olusturur, yayimlar ve sonuclari gorur.
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ile korunuyor, ekstra anotasyona gerek yok.
 */
@Service
public class AdminSurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminSurveyService(SurveyRepository surveyRepository,
                               SurveyResponseRepository surveyResponseRepository,
                               FeedbackRepository feedbackRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** GET /admin/surveys — admin UI'da anket secip yayimlamak/sonuc gormek icin liste. */
    @Transactional(readOnly = true)
    public List<AdminSurveyResponse> listAll() {
        return surveyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminSurveyResponse::from)
                .toList();
    }

    /** POST /admin/surveys — taslak (published=false) olarak olusturur. */
    @Transactional
    public AdminSurveyResponse createSurvey(Integer adminEmployeeId, AdminSurveyCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Anket başlığı boş olamaz");
        }

        Survey survey = new Survey();
        survey.setTitle(request.title());
        survey.setCreatedBy(adminEmployeeId);
        survey.setCreatedAt(LocalDateTime.now());
        survey.setPublished(false);

        return AdminSurveyResponse.from(surveyRepository.save(survey));
    }

    /** PUT /admin/surveys/{id}/publish — yayimlar, GET /surveys/active'te gorunur hale gelir. */
    @Transactional
    public AdminSurveyResponse publish(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));
        survey.setPublished(true);
        return AdminSurveyResponse.from(surveyRepository.save(survey));
    }

    /** GET /admin/surveys/{id}/results — FR-44: sonuclari ozet halinde dondurur. */
    @Transactional(readOnly = true)
    public SurveyResultsResponse getResults(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));

        List<SurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(surveyId);
        List<Feedback> feedbacks = feedbackRepository.findAllBySurveyId(surveyId);

        Map<String, Map<String, Long>> answerCounts = new HashMap<>();
        for (SurveyResponse response : responses) {
            if (response.getAnswers() == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : response.getAnswers().entrySet()) {
                String question = entry.getKey();
                String answer = String.valueOf(entry.getValue());
                answerCounts
                        .computeIfAbsent(question, q -> new HashMap<>())
                        .merge(answer, 1L, Long::sum);
            }
        }

        List<String> feedbackComments = feedbacks.stream()
                .map(Feedback::getContent)
                .filter(content -> content != null && !content.isBlank())
                .toList();

        return new SurveyResultsResponse(
                survey.getId(),
                survey.getTitle(),
                survey.isPublished(),
                responses.size(),
                feedbacks.size(),
                answerCounts,
                feedbackComments);
    }
}
