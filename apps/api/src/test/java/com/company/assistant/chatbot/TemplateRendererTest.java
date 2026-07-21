package com.company.assistant.chatbot;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    void cokluPlaceholderDogruDoldurulur() {
        var sonuc = renderer.render(
                "Merhaba {{kullanici_adi}}, {{departman}} departmanindasin.",
                Map.of("kullanici_adi", "Mustafa", "departman", "IT"));
        assertThat(sonuc).isEqualTo("Merhaba Mustafa, IT departmanindasin.");
    }

    @Test
    void ayniPlaceholderBirdenCokKezGecebilir() {
        var sonuc = renderer.render(
                "{{ad}} hos geldin! Tekrar merhaba {{ad}}.",
                Map.of("ad", "Zeynep"));
        assertThat(sonuc).isEqualTo("Zeynep hos geldin! Tekrar merhaba Zeynep.");
    }

    @Test
    void bilinmeyenPlaceholderOlduguGibiKalir() {
        var sonuc = renderer.render(
                "Merhaba {{kullanici_adi}}, kodun: {{bilinmeyen_alan}}",
                Map.of("kullanici_adi", "Mustafa"));
        assertThat(sonuc).isEqualTo("Merhaba Mustafa, kodun: {{bilinmeyen_alan}}");
    }

    @Test
    void dolarIsaretiIcerenDegerBozulmadanYazilir() {
        // Regresyon korumasi: appendReplacement'ta '$' grup referansi sayilir.
        // Matcher.quoteReplacement kaldirilirsa bu test IllegalArgumentException ile kirilir.
        var sonuc = renderer.render(
                "Maas bilgin: {{tutar}}",
                Map.of("tutar", "$1.250"));
        assertThat(sonuc).isEqualTo("Maas bilgin: $1.250");
    }

    @Test
    void tersBolulIcerenDegerBozulmadanYazilir() {
        // '\' de appendReplacement icin ozel karakterdir, ayni korumanin parcasi.
        var sonuc = renderer.render(
                "Dosya yolu: {{yol}}",
                Map.of("yol", "C:\\dev\\proje"));
        assertThat(sonuc).isEqualTo("Dosya yolu: C:\\dev\\proje");
    }

    @Test
    void placeholderYoksaTemplateAynenDoner() {
        var sonuc = renderer.render("Sabit bir yanit metni.", Map.of());
        assertThat(sonuc).isEqualTo("Sabit bir yanit metni.");
    }
}