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
 * C-13 (#121): olusturmada sabit secenek listesi (min 2) + opsiyonel deadline zorunlu oldu.
 */
@Service
public class AdminSurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminSurveyService(SurveyRepository surveyRepository,
                               SurveyOptionRepository surveyOptionRepository,
                               SurveyResponseRepository surveyResponseRepository,
                               FeedbackRepository feedbackRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyOptionRepository = surveyOptionRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** GET /admin/surveys — admin UI'da anket secip yayimlamak/sonuc gormek icin liste. */
    @Transactional(readOnly = true)
    public List<AdminSurveyResponse> listAll() {
        return surveyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(survey -> AdminSurveyResponse.from(survey,
                        surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(survey.getId())))
                .toList();
    }

    /**
     * POST /admin/surveys — taslak (published=false) olarak olusturur.
     * C-13 (#121): en az 2 secenek zorunlu, deadline opsiyonel (null olabilir - hep acik anlamina gelir).
     */
    @Transactional
    public AdminSurveyResponse createSurvey(Integer adminEmployeeId, AdminSurveyCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Anket başlığı boş olamaz");
        }
        List<String> options = request.options();
        if (options == null || options.stream().filter(o -> o != null && !o.isBlank()).count() < 2) {
            throw new IllegalArgumentException("En az 2 seçenek girilmeli");
        }

        Survey survey = new Survey();
        survey.setTitle(request.title());
        survey.setCreatedBy(adminEmployeeId);
        survey.setCreatedAt(LocalDateTime.now());
        survey.setPublished(false);
        survey.setDeadline(request.deadline());
        survey = surveyRepository.save(survey);

        int order = 0;
        for (String optionText : options) {
            if (optionText == null || optionText.isBlank()) {
                continue;
            }
            SurveyOption option = new SurveyOption();
            option.setSurvey(survey);
            option.setOptionText(optionText.trim());
            option.setSortOrder(order++);
            surveyOptionRepository.save(option);
        }

        return AdminSurveyResponse.from(survey,
                surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(survey.getId()));
    }

    /**
     * PUT /admin/surveys/{id} — C-13 (#121): baslik, secenekler ve gecerlilik (deadline) tarihi duzenlenir.
     * Secenekler tamamen yenisiyle degistirilir (eskiler silinir) - mevcut yanitlar (survey_response)
     * option_id'ye referans verdigi icin secenekleri degistirmek anketi yayimdan once, taslakken yapilmali;
     * yayimlanmis bir ankette secenek degisikligi mevcut sonuclari bozabilir, bu bilerek engellenmiyor
     * (admin'in sorumlulugunda - ileride kisitlanabilir).
     */
    @Transactional
    public AdminSurveyResponse updateSurvey(Integer surveyId, AdminSurveyUpdateRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));

        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Anket başlığı boş olamaz");
        }
        List<String> options = request.options();
        if (options == null || options.stream().filter(o -> o != null && !o.isBlank()).count() < 2) {
            throw new IllegalArgumentException("En az 2 seçenek girilmeli");
        }

        survey.setTitle(request.title());
        survey.setDeadline(request.deadline());
        survey = surveyRepository.save(survey);

        surveyOptionRepository.deleteAllBySurveyId(surveyId);
        int order = 0;
        for (String optionText : options) {
            if (optionText == null || optionText.isBlank()) {
                continue;
            }
            SurveyOption option = new SurveyOption();
            option.setSurvey(survey);
            option.setOptionText(optionText.trim());
            option.setSortOrder(order++);
            surveyOptionRepository.save(option);
        }

        return AdminSurveyResponse.from(survey,
                surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(survey.getId()));
    }

    /**
     * DELETE /admin/surveys/{id} — C-13 (#121): anketi ve bagli tum kayitlari (secenekler,
     * yanitlar, anonim geri bildirimler) siler. FK constraint'leri yuzunden sirali silinir.
     */
    @Transactional
    public void deleteSurvey(Integer surveyId) {
        if (!surveyRepository.existsById(surveyId)) {
            throw new SurveyNotFoundException("Anket bulunamadı: " + surveyId);
        }
        surveyResponseRepository.deleteAllBySurveyId(surveyId);
        feedbackRepository.deleteAllBySurveyId(surveyId);
        surveyOptionRepository.deleteAllBySurveyId(surveyId);
        surveyRepository.deleteById(surveyId);
    }

    /** PUT /admin/surveys/{id}/publish — yayimlar, GET /surveys/active'te gorunur hale gelir. */
    @Transactional
    public AdminSurveyResponse publish(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));
        survey.setPublished(true);
        survey = surveyRepository.save(survey);
        return AdminSurveyResponse.from(survey,
                surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(survey.getId()));
    }

    /**
     * GET /admin/surveys/{id}/results — FR-44: sonuclari ozet halinde dondurur.
     * C-13 (#121): answerCounts artik "secenek metni -> oy sayisi" (tek soruluk anket).
     * Hic oy almayan secenekler de 0 ile listede gorunur (grafik/bar icin).
     */
    @Transactional(readOnly = true)
    public SurveyResultsResponse getResults(Integer surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));

        List<SurveyOption> options = surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(surveyId);
        List<SurveyResponse> responses = surveyResponseRepository.findAllBySurveyId(surveyId);
        List<Feedback> feedbacks = feedbackRepository.findAllBySurveyId(surveyId);

        Map<String, Long> answerCounts = new HashMap<>();
        for (SurveyOption option : options) {
            answerCounts.put(option.getOptionText(), 0L);
        }
        for (SurveyResponse response : responses) {
            if (response.getOption() == null) {
                continue;
            }
            answerCounts.merge(response.getOption().getOptionText(), 1L, Long::sum);
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
