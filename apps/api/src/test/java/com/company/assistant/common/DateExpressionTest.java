package com.company.assistant.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * A-37 (#203): acik tarih ifadelerinin tespiti ve cozumu.
 *
 * <p>Testin ayirt ettigi asil sey iki durumun BIRBIRINDEN farki: "tarih ifadesi yok"
 * (cagiran varsayilana duser, dogru) ile "tarih ifadesi var ama cozulemedi" (cagiran
 * varsayilana DUSMEMELI). Resolver'lardaki hata tam olarak bu ikisini ayirt etmemekti.
 */
class DateExpressionTest {

    private static final LocalDate REFERANS = LocalDate.of(2026, 8, 10);

    @Test
    void gunVeAyAdi() {
        assertThat(DateExpression.resolve("17 agustos yemek menusu", REFERANS))
                .contains(LocalDate.of(2026, 8, 17));
    }

    @Test
    void ayAdiVeGun() {
        assertThat(DateExpression.resolve("agustos 17 menusu", REFERANS))
                .contains(LocalDate.of(2026, 8, 17));
    }

    // Yil belirtilmemisse referans yil kullanilir.
    @Test
    void yilBelirtilmemis() {
        assertThat(DateExpression.resolve("3 eylul", REFERANS))
                .contains(LocalDate.of(2026, 9, 3));
    }

    @Test
    void yilBelirtilmis() {
        assertThat(DateExpression.resolve("17 agustos 2027", REFERANS))
                .contains(LocalDate.of(2027, 8, 17));
    }

    @Test
    void sayisalBicim() {
        assertThat(DateExpression.resolve("17.08.2026 menusu", REFERANS))
                .contains(LocalDate.of(2026, 8, 17));
        assertThat(DateExpression.resolve("17/08", REFERANS))
                .contains(LocalDate.of(2026, 8, 17));
    }

    @Test
    void ikiHaneliYil() {
        assertThat(DateExpression.resolve("17.08.27", REFERANS))
                .contains(LocalDate.of(2027, 8, 17));
    }

    /**
     * Yalniz ay adi bir tarih ifadesidir ama hangi gun oldugu belirsiz. Cagiran taraf bugune
     * DUSMEMELI; kullaniciya sorulanin anlasilmadigi soylenmeli.
     */
    @Test
    void yalnizAyAdi_tespitEdilirAmaCozulemez() {
        assertThat(DateExpression.mentionsDate("agustos menusu")).isTrue();
        assertThat(DateExpression.resolve("agustos menusu", REFERANS)).isEmpty();
    }

    @Test
    void gecersizGun_tespitEdilirAmaCozulemez() {
        assertThat(DateExpression.mentionsDate("32 agustos")).isTrue();
        assertThat(DateExpression.resolve("32 agustos", REFERANS)).isEmpty();
    }

    /**
     * Goreli ifadeler BU SINIFIN isi degil — resolver'lar onlari zaten cozuyor ve
     * calisiyorlar. Buraya dahil edilseydi mevcut dogru davranis bozulurdu.
     */
    @Test
    void goreliIfadelerTarihSayilmaz() {
        assertThat(DateExpression.mentionsDate("yarin ne var")).isFalse();
        assertThat(DateExpression.mentionsDate("bugun yemekte ne var")).isFalse();
        assertThat(DateExpression.mentionsDate("gelecek hafta carsamba")).isFalse();
        assertThat(DateExpression.mentionsDate("3 gun sonra")).isFalse();
    }

    @Test
    void bosGirdi() {
        assertThat(DateExpression.mentionsDate(null)).isFalse();
        assertThat(DateExpression.resolve(null, REFERANS)).isEmpty();
    }
}
