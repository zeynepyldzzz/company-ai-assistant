package com.company.assistant.survey;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * A-18: 'anket' intent'i icin aktif anket listesi (FR-42).
 *
 * Uretilen anahtar V28 template'iyle eslesir:
 *   {{aktif_anketler}} -> anket basliklari + olusturulma tarihi
 *
 * SurveyService.getActiveSurveys() yalnizca published=true anketleri dondurur (C-8/#52);
 * yayimlanmamis anket chatbot'tan ASLA gorunmez.
 *
 * Bilerek yapilmayanlar:
 *   - Anket sonuclari / katilim sayisi: FR-44 geregi yetkili kullaniciya ait, employee
 *     roluyle konusan chatbot'tan acilmaz.
 *   - Ankete cevap gonderme: yazma islemi, ilgili ekranda yapilir.
 *   - Son tarih: survey tablosunda boyle bir kolon yok, uydurulmaz.
 */
@Component
public class SurveyVariableResolver {

    private static final String SURVEY_INTENT = "anket";
    private static final String VARIABLE = "aktif_anketler";

    private static final String NO_SURVEY =
            "Şu anda katılabileceğin aktif bir anket görünmüyor.";
    private static final String FOOTER =
            "\n\nAnketlere Anketler bölümünden katılabilirsin.";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    private final SurveyService surveyService;

    public SurveyVariableResolver(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    public Map<String, String> resolve(String intentName) {
        if (!SURVEY_INTENT.equals(intentName)) {
            return Map.of();
        }

        List<SurveyDto> surveys = surveyService.getActiveSurveys();
        if (surveys.isEmpty()) {
            return Map.of(VARIABLE, NO_SURVEY);
        }

        String body = surveys.stream()
                .map(survey -> "• " + survey.title()
                        + (survey.createdAt() != null ? " — " + survey.createdAt().format(DATE_FMT) : ""))
                .collect(Collectors.joining("\n"));

        return Map.of(VARIABLE, "Aktif anketler:\n" + body + FOOTER);
    }
}
