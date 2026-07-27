package com.company.assistant.survey;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import jakarta.persistence.Query;

import static org.assertj.core.api.Assertions.assertThat;

// C-7 (#51) kabul kriteri: "POST /feedback ile gonderilen kayitta employeeId
// alani yok (FR-43 anonimlik — sema zaten garanti ediyor, API de bunu yansitmali)".
//
// Bu test IKI seviyede kanitlar:
// 1) Semada gercekten employee_id kolonu YOK (information_schema sorgusu).
// 2) SurveyService.submitFeedback ile gercek bir kayit yazilabiliyor ve
//    hicbir employee referansi olmadan basariyla persist ediliyor.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SurveyService.class})
class FeedbackAnonymityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private FeedbackRepository feedbackRepository;

    // C-T3 (#54): FR-43 kritik gizlilik kurali icin request govdesinde de
    // employeeId alaninin bulunmadigini (yalnizca DB semasinda degil) acikca
    // dogrular. FeedbackRequest'e ileride yanlislikla employeeId eklenirse
    // bu test kirilir.
    @Test
    void feedbackRequest_employeeIdAlaniIcermez() {
        List<String> alanlar = Arrays.stream(FeedbackRequest.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(alanlar).doesNotContain("employeeId");
    }

    @Test
    void feedbackTablosunda_employeeIdKolonuYok() {
        Query query = entityManager.getEntityManager().createNativeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'feedback' AND column_name = 'employee_id'");

        @SuppressWarnings("unchecked")
        List<Object> sonuc = query.getResultList();

        assertThat(sonuc).isEmpty();
    }

    @Test
    void feedbackGonderilir_kimlikBilgisiOlmadanKaydedilir() {
        FeedbackRequest request = new FeedbackRequest(null, "harika bir anket, tesekkurler");

        surveyService.submitFeedback(request);
        entityManager.flush();
        entityManager.clear();

        List<Feedback> hepsi = feedbackRepository.findAll();
        assertThat(hepsi).hasSize(1);
        assertThat(hepsi.get(0).getContent()).isEqualTo("harika bir anket, tesekkurler");
        // Feedback entity'sinde employeeId alani zaten YOK (compile-time garanti);
        // burada sadece kaydin basariyla ve icerikle olustugunu dogruluyoruz.
    }
}
