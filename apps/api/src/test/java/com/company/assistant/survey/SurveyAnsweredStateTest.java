package com.company.assistant.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-33 (#192): aktif anket listesindeki {@code answered} alani.
 *
 * <p>Bu alan olmadan istemci "Yanitla" formunu tekrar aciyor, kullanici cevabi
 * gonderdiginde sunucudan 409 aliyordu. Mukerrer yanit DB'de {@code (survey_id,
 * employee_id)} unique kisitiyla zaten engelliydi (C-13/#121) — eksik olan, istemcinin
 * bunu ONCEDEN bilmesiydi.
 */
@ExtendWith(MockitoExtension.class)
class SurveyAnsweredStateTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyOptionRepository surveyOptionRepository;
    @Mock
    private SurveyResponseRepository surveyResponseRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    private SurveyService service;

    @BeforeEach
    void setUp() {
        service = new SurveyService(surveyRepository, surveyOptionRepository,
                surveyResponseRepository, feedbackRepository);
    }

    private Survey survey(int id, String title) {
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitle(title);
        survey.setPublished(true);
        return survey;
    }

    private void stubActiveSurveys(Survey... surveys) {
        when(surveyRepository.findAllByPublishedTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(surveys));
        when(surveyOptionRepository.findAllBySurveyIdOrderBySortOrderAsc(anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void yanitlananAnketAnsweredTrueDoner_digerleriFalse() {
        stubActiveSurveys(survey(1, "Kantin anketi"), survey(2, "Servis anketi"));
        when(surveyResponseRepository.findAnsweredSurveyIds(42)).thenReturn(List.of(1));

        List<SurveyDto> result = service.getActiveSurveys(42);

        assertThat(result).extracting(SurveyDto::id, SurveyDto::answered)
                .containsExactly(tuple(1, true), tuple(2, false));
    }

    @Test
    void hicYanitVermeyenIcinHepsiFalse() {
        stubActiveSurveys(survey(1, "Kantin anketi"));
        when(surveyResponseRepository.findAnsweredSurveyIds(42)).thenReturn(List.of());

        assertThat(service.getActiveSurveys(42).get(0).answered()).isFalse();
    }

    /**
     * Kimliksiz imza chatbot icin duruyor ({@code SurveyVariableResolver} kullanici kimligi
     * tasimiyor). Orada "cevapladin mi" bilgisi kullanilmadigi icin sorgu HIC calismamali —
     * aksi halde her chatbot yanitinda gereksiz bir sorgu atilirdi.
     */
    @Test
    void kimliksizCagriYanitSorgusuYapmaz() {
        stubActiveSurveys(survey(1, "Kantin anketi"));

        List<SurveyDto> result = service.getActiveSurveys();

        assertThat(result.get(0).answered()).isFalse();
        verify(surveyResponseRepository, never()).findAnsweredSurveyIds(anyInt());
    }
}
