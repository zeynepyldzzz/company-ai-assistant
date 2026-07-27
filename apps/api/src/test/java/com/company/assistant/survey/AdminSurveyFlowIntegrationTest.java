package com.company.assistant.survey;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.company.assistant.directory.Employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// C-8 (#52) uctan uca akis: admin taslak anket olusturur -> yayimlar ->
// calisanlar yanit gonderir -> admin sonuclari ozet halinde gorur (FR-44, FR-76).
// Ayrica C-7'deki (#51) duzeltilen davranisi kanitlar: yayimlanmamis (taslak)
// bir anket GET /surveys/active listesinde GORUNMEZ ve yanit KABUL ETMEZ.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AdminSurveyService.class, SurveyService.class})
class AdminSurveyFlowIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AdminSurveyService adminSurveyService;

    @Autowired
    private SurveyService surveyService;

    private Employee calisanOlustur(String email) {
        Employee e = new Employee();
        e.setName("Test Calisan");
        e.setEmail(email);
        entityManager.persistAndFlush(e);
        return e;
    }

    @Test
    void taslakAnket_aktifListedeGorunmezVeYanitKabulEtmez() {
        Employee admin = calisanOlustur("admin-c8-1@example.com");
        Employee calisan = calisanOlustur("calisan-c8-1@example.com");

        AdminSurveyResponse taslak = adminSurveyService.createSurvey(
                admin.getId(), new AdminSurveyCreateRequest("Taslak Anket"));
        assertThat(taslak.published()).isFalse();

        // GET /surveys/active — taslak anket LISTEDE OLMAMALI.
        List<SurveyDto> aktifListe = surveyService.getActiveSurveys();
        assertThat(aktifListe).noneMatch(s -> s.id().equals(taslak.id()));

        // POST /surveys/{id}/responses — taslak ankete yanit reddedilmeli.
        assertThatThrownBy(() -> surveyService.submitResponse(
                taslak.id(), calisan.getId(), new SurveyResponseRequest(Map.of("q1", "evet"))))
                .isInstanceOf(SurveyNotPublishedException.class);
    }

    @Test
    void yayimlananAnket_yanitAlirVeAdminOzetGorur() {
        Employee admin = calisanOlustur("admin-c8-2@example.com");
        Employee calisan1 = calisanOlustur("calisan-c8-2@example.com");
        Employee calisan2 = calisanOlustur("calisan-c8-3@example.com");

        AdminSurveyResponse olusturulan = adminSurveyService.createSurvey(
                admin.getId(), new AdminSurveyCreateRequest("Memnuniyet Anketi"));
        AdminSurveyResponse yayimlanan = adminSurveyService.publish(olusturulan.id());
        assertThat(yayimlanan.published()).isTrue();

        // Artik aktif listede gorunmeli.
        List<SurveyDto> aktifListe = surveyService.getActiveSurveys();
        assertThat(aktifListe).anyMatch(s -> s.id().equals(yayimlanan.id()));

        // Iki calisan yanit gonderir.
        surveyService.submitResponse(yayimlanan.id(), calisan1.getId(),
                new SurveyResponseRequest(Map.of("memnun_musunuz", "evet")));
        surveyService.submitResponse(yayimlanan.id(), calisan2.getId(),
                new SurveyResponseRequest(Map.of("memnun_musunuz", "evet")));

        // Bir de anonim geri bildirim (feedback bu ankete bagli).
        surveyService.submitFeedback(new FeedbackRequest(yayimlanan.id(), "cok iyi bir anketti"));

        entityManager.flush();
        entityManager.clear();

        // Admin FR-44: sonuclari ozet halinde gorur.
        SurveyResultsResponse sonuclar = adminSurveyService.getResults(yayimlanan.id());
        assertThat(sonuclar.totalResponses()).isEqualTo(2);
        assertThat(sonuclar.totalFeedback()).isEqualTo(1);
        assertThat(sonuclar.answerCounts())
                .containsEntry("memnun_musunuz", Map.of("evet", 2L));
        assertThat(sonuclar.feedbackComments()).containsExactly("cok iyi bir anketti");
    }
}
