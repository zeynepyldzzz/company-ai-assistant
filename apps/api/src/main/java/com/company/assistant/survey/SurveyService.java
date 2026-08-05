package com.company.assistant.survey;

import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-7 (#51): GET /surveys/active, POST /surveys/{id}/responses, POST /feedback.
 * C-8 (#52): admin taslak/yayimlama akisi icin published kontrolu eklendi.
 * C-13 (#121): sabit secenek + deadline + tekil oy kisiti.
 */
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final FeedbackRepository feedbackRepository;

    public SurveyService(SurveyRepository surveyRepository,
                          SurveyOptionRepository surveyOptionRepository,
                          SurveyResponseRepository surveyResponseRepository,
                          FeedbackRepository feedbackRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyOptionRepository = surveyOptionRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Kimliksiz cagri — {@code answered} her zaman false doner.
     *
     * <p>A-33 (#192): chatbot ({@code SurveyVariableResolver}) aktif anketleri listelerken
     * kullanici kimligini tasimiyor. O yanitta "cevapladin mi" bilgisi zaten kullanilmadigi
     * icin ayri bir imza birakildi; alternatif, kimligi chatbot zincirinin tamamindan
     * gecirmekti ve bu issue'nun kapsamini asardi.
     */
    @Transactional(readOnly = true)
    public List<SurveyDto> getActiveSurveys() {
        return buildActiveSurveys(List.of());
    }

    /**
     * GET /surveys/active — FR-42. C-8 (#52): sadece published=true anketler doner.
     *
     * <p>A-33 (#192): kimlik JWT'den gelir (FR-63 deseni, {@code submitResponse} ile ayni
     * kural), istek govdesinden veya URL'den DEGIL — aksi halde bir kullanici baskasinin
     * hangi anketleri yanitladigini ogrenebilirdi.
     */
    @Transactional(readOnly = true)
    public List<SurveyDto> getActiveSurveys(Integer employeeId) {
        return buildActiveSurveys(surveyResponseRepository.findAnsweredSurveyIds(employeeId));
    }

    private List<SurveyDto> buildActiveSurveys(List<Integer> answeredSurveyIds) {
        Set<Integer> answered = Set.copyOf(answeredSurveyIds);
        return surveyRepository.findAllByPublishedTrueOrderByCreatedAtDesc().stream()
                .map(survey -> SurveyDto.from(
                        survey,
                        surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(survey.getId()),
                        answered.contains(survey.getId())))
                .toList();
    }

    /**
     * POST /surveys/{id}/responses — FR-42.
     * FR-63'teki pattern ile ayni: kimlik JWT'den (authentication.getName()) gelir,
     * govdeden degil; o yuzden employeeId parametre olarak controller'dan aliniyor.
     * C-13 (#121): deadline gecmisse ve secilen option baska bir ankete aitse hata,
     * ayni calisan ikinci kez oy vermeye calisirsa DB unique constraint'i 409'a cevrilir.
     */
    @Transactional
    public void submitResponse(Integer surveyId, Integer employeeId, SurveyResponseRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new SurveyNotFoundException("Anket bulunamadı: " + surveyId));

        // C-8 (#52): taslak (yayimlanmamis) bir ankete yanit verilemez.
        if (!survey.isPublished()) {
            throw new SurveyNotPublishedException("Anket henüz yayımlanmamış: " + surveyId);
        }

        // C-13 (#121): deadline gecmisse yeni yanit kabul edilmez.
        if (survey.isExpired()) {
            throw new SurveyDeadlinePassedException("Anketin son yanıt tarihi geçti: " + surveyId);
        }

        if (request == null || request.optionId() == null) {
            throw new IllegalArgumentException("Bir seçenek seçilmeli");
        }

        SurveyOption option = surveyOptionRepository.findById(request.optionId())
                .orElseThrow(() -> new IllegalArgumentException("Seçenek bulunamadı: " + request.optionId()));
        if (!option.getSurvey().getId().equals(surveyId)) {
            throw new IllegalArgumentException("Seçenek bu ankete ait değil: " + request.optionId());
        }

        SurveyResponse response = new SurveyResponse();
        response.setSurvey(survey);
        response.setEmployeeId(employeeId);
        response.setOption(option);

        try {
            surveyResponseRepository.save(response);
            surveyResponseRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // C-13 (#121): (survey_id, employee_id) unique constraint'i tetiklendi -
            // bu calisan bu ankete daha once oy vermis.
            throw new SurveyAlreadyRespondedException("Bu ankete daha önce yanıt verdiniz: " + surveyId);
        }
    }

    /**
     * GET /surveys/{id}/response-count — C-13 (#121): calisana acik, sonuc detayi
     * olmadan sadece toplam yanit sayisini dondurur (dashboard progress bar icin).
     */
    @Transactional(readOnly = true)
    public SurveyResponseCountResponse getResponseCount(Integer surveyId) {
        if (!surveyRepository.existsById(surveyId)) {
            throw new SurveyNotFoundException("Anket bulunamadı: " + surveyId);
        }
        return new SurveyResponseCountResponse(surveyId, surveyResponseRepository.countBySurveyId(surveyId));
    }

    /**
     * POST /feedback — FR-43: anonim geri bildirim.
     * BILINCLI OLARAK employeeId ALINMAZ/KAYDEDILMEZ — anonimlik sema
     * seviyesinde (feedback tablosunda employee_id kolonu yok) garanti edilir.
     * C-13 (#121): "sikli soruya oy ver + istersen anonim yorum birak" akisinda
     * frontend oy verdikten sonra ayrica bu endpoint'i cagirir (surveyId ile iliskilendirilir,
     * kimlik hic gonderilmez).
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
        feedback.setCreatedAt(java.time.LocalDateTime.now());
        feedbackRepository.save(feedback);
    }
}
