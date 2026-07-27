package com.company.assistant.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A-T2 / AC3: GET /hr/procedures?topic= (getByTopic) en az 3 farkli topic icin DOGRU
 * proseduru donduruyor (FR-51-57). Gercek DB + seed (V15) uzerinden cozunurluk testi;
 * controller mock'lu HrProcedureControllerTest yalnizca yonlendirmeyi dogruluyordu.
 */
@SpringBootTest
class HrProcedureTopicIntegrationTest {

    @Autowired
    private HrProcedureService service;

    @ParameterizedTest
    @CsvSource({
            "onboarding,   İşe Giriş ve Oryantasyon Prosedürü",
            "izin,         Yıllık İzin Prosedürü",
            "fazla-mesai,  Fazla Mesai Prosedürü",
            "mazeret-izni, Mazeret İzni Prosedürü"
    })
    void topic_dogruProseduruDoner(String topic, String beklenenBaslik) {
        var detay = service.getByTopic(topic);

        assertThat(detay.category()).isEqualTo(topic);
        assertThat(detay.title()).isEqualTo(beklenenBaslik);
    }

    @Test
    void gecersizTopic_notFoundFirlatir() {
        assertThatThrownBy(() -> service.getByTopic("olmayan-topic"))
                .isInstanceOf(HrProcedureNotFoundException.class);
    }
}
