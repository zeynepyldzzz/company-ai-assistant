package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

/** GET /surveys/active listesindeki tek anket (calisan tarafi). */
public record SurveyDto(Integer id, String title, LocalDateTime createdAt, LocalDateTime deadline,
                         List<SurveyOptionDto> options, boolean answered) {

    /**
     * A-33 (#192): {@code answered} — bu anketi ISTEGI YAPAN calisan yanitladi mi.
     *
     * <p>Bu alan olmadan istemci "cevapla" formunu tekrar aciyordu; kullanici cevabi
     * gonderdiginde sunucudan 409 donuyordu (mukerrer yanit DB'de {@code (survey_id,
     * employee_id)} unique kisitiyla zaten engelli, C-13/#121). Yani sistem dogru
     * davraniyor ama kullaniciya bozuk gorunuyordu: engellenmesi gereken bir islem
     * yapilabiliyor, sonunda hata gosteriliyordu.
     */
    static SurveyDto from(Survey survey, List<SurveyOption> options, boolean answered) {
        return new SurveyDto(
                survey.getId(),
                survey.getTitle(),
                survey.getCreatedAt(),
                survey.getDeadline(),
                options.stream().map(SurveyOptionDto::from).toList(),
                answered);
    }
}
