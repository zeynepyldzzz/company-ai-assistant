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
// C-13 (#121): sabit secenek + tekil oy kisiti (ayni calisan ikinci kez oy veremez).
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
                admin.getId(), new AdminSurveyCreateRequest("Taslak Anket", null, List.of("Evet", "Hayır")));
        assertThat(taslak.published()).isFalse();
        Integer optionId = taslak.options().get(0).id();

        // GET /surveys/active — taslak anket LISTEDE OLMAMALI.
        List<SurveyDto> aktifListe = surveyService.getActiveSurveys();
        assertThat(aktifListe).noneMatch(s -> s.id().equals(taslak.id()));

        // POST /surveys/{id}/responses — taslak ankete yanit reddedilmeli.
        assertThatThrownBy(() -> surveyService.submitResponse(
                taslak.id(), calisan.getId(), new SurveyResponseRequest(optionId)))
                .isInstanceOf(SurveyNotPublishedException.class);
    }

    @Test
    void yayimlananAnket_yanitAlirVeAdminOzetGorur() {
        Employee admin = calisanOlustur("admin-c8-2@example.com");
        Employee calisan1 = calisanOlustur("calisan-c8-2@example.com");
        Employee calisan2 = calisanOlustur("calisan-c8-3@example.com");

        AdminSurveyResponse olusturulan = adminSurveyService.createSurvey(
                admin.getId(), new AdminSurveyCreateRequest("Memnuniyet Anketi", null, List.of("Evet", "Hayır")));
        AdminSurveyResponse yayimlanan = adminSurveyService.publish(olusturulan.id());
        assertThat(yayimlanan.published()).isTrue();
        Integer evetOptionId = yayimlanan.options().get(0).id();

        // Artik aktif listede gorunmeli.
        List<SurveyDto> aktifListe = surveyService.getActiveSurveys();
        assertThat(aktifListe).anyMatch(s -> s.id().equals(yayimlanan.id()));

        // Iki calisan yanit gonderir (ayni secenegi).
        surveyService.submitResponse(yayimlanan.id(), calisan1.getId(), new SurveyResponseRequest(evetOptionId));
        surveyService.submitResponse(yayimlanan.id(), calisan2.getId(), new SurveyResponseRequest(evetOptionId));

        // Bir de anonim geri bildirim (feedback bu ankete bagli).
        surveyService.submitFeedback(new FeedbackRequest(yayimlanan.id(), "cok iyi bir anketti"));

        entityManager.flush();
        entityManager.clear();

        // Admin FR-44: sonuclari ozet halinde gorur.
        SurveyResultsResponse sonuclar = adminSurveyService.getResults(yayimlanan.id());
        assertThat(sonuclar.totalResponses()).isEqualTo(2);
        assertThat(sonuclar.totalFeedback()).isEqualTo(1);
        assertThat(sonuclar.answerCounts()).containsEntry("Evet", 2L);
        assertThat(sonuclar.feedbackComments()).containsExactly("cok iyi bir anketti");

        // C-13 (#121): calisan progress-bar icin toplam yanit sayisini gorebilir.
        SurveyResponseCountResponse count = surveyService.getResponseCount(yayimlanan.id());
        assertThat(count.totalResponses()).isEqualTo(2);
    }

    // C-13 (#121): ayni calisan ayni ankete ikinci kez oy veremez (409 -> AlreadyResponded).
    @Test
    void ayniCalisanIkinciKezOyVerirse_409Doner() {
        Employee admin = calisanOlustur("admin-c13-1@example.com");
        Employee calisan = calisanOlustur("calisan-c13-1@example.com");

        AdminSurveyResponse olusturulan = adminSurveyService.createSurvey(
                admin.getId(), new AdminSurveyCreateRequest("Tekil Oy Testi", null, List.of("A", "B")));
        AdminSurveyResponse yayimlanan = adminSurveyService.publish(olusturulan.id());
        Integer optionId = yayimlanan.options().get(0).id();

        surveyService.submitResponse(yayimlanan.id(), calisan.getId(), new SurveyResponseRequest(optionId));
        entityManager.flush();

        assertThatThrownBy(() -> {
            surveyService.submitResponse(yayimlanan.id(), calisan.getId(), new SurveyResponseRequest(optionId));
        }).isInstanceOf(SurveyAlreadyRespondedException.class);
    }
}
