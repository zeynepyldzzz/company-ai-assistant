package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Gercek Ollama + pgvector ile uctan uca dogruluk testi.
 * Sadece INTENT_IT=true ortam degiskeni varsa kosar (CI'da skip).
 * Kosul: docker compose up -d (db + ollama) ve seed'in tamamlanmis olmasi.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "INTENT_IT", matches = "true")
class IntentClassificationIT {

    @Autowired
    private IntentClassificationService service;

    @ParameterizedTest
    @CsvSource({
            "bugün yemekhanede ne çıkacak,      yemek_menusu",
            "öğlen ne yenecek,                  yemek_menusu",
            "servis sabah kaçta geliyor,        servis_saatleri",
            "servise nereden binebilirim,       servis_guzergah",
            "İK'ya nasıl ulaşabilirim,          rehber_departman",
            "Mehmet Beyin dahilisi kaç,         rehber_kisi",
            "yeni bir duyuru yayınlandı mı,     duyurular",
            "yıllık iznimi nasıl kullanırım,    izin_prosedur",
            "mesai ücretini kim onaylıyor,      fazla_mesai",
            "işe yeni girdim ne yapmalıyım,     ise_giris_oryantasyon",
            "selam naber,                       selamlama"
    })
    void ornekSorularDogruIntenteSiniflanir(String soru, String beklenenIntent) {
        var result = service.classify(soru);
        assertThat(result.intent())
                .as("soru: '%s' (benzerlik: %.3f, eslesen: '%s')",
                        soru, result.similarity(), result.matchedPhrase())
                .isEqualTo(beklenenIntent);
    }

    @ParameterizedTest
    @CsvSource({
            "bitcoin fiyatı ne kadar",
            "araba lastiği nereden alınır",
            "en iyi dizi önerisi ver"
    })
    void alakasizSorularFallbackeDuser(String soru) {
        var result = service.classify(soru);
        assertThat(result.matched())
                .as("soru: '%s' -> intent: '%s' (benzerlik: %.3f)",
                        soru, result.intent(), result.similarity())
                .isFalse();
    }
    @Test
    @EnabledIfEnvironmentVariable(named = "INTENT_IT", matches = "true")
    void bilinenSinirlama_kalipBenzerligiYanlisPozitifUretebilir() {
        // "yarın ... olacak" kalibi, anlamsal fark buyuk olsa da yemek orneklerine
        // yuksek benzerlik uretiyor (0.73+). Embedding-only yaklasimin bilinen siniri;
        // Faz 2'de cross-encoder/re-ranking ile ele alinabilir. Bu test davranisi
        // BELGELER, dogrulugunu iddia etmez — davranis degisirse (iyilesirse) kirilir
        // ve bilerek guncellenir.
        var result = service.classify("yarın hava nasıl olacak");
        assertThat(result.intent()).isEqualTo("yemek_menusu"); // bilinen yanlis pozitif
    }
}