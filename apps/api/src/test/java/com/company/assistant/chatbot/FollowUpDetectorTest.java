package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * A-37 (#203): takip sorusu tespiti.
 *
 * <p>Bu testlerin asil isi kuralin GENISLEMESINI onlemek. Kural bilerek dar: yalnizca mesajin
 * tamami zaman ifadesinden ibaretse baglam devreye girer. Genisletilirse ("kisa mesajlarda da
 * baglam kullan" gibi) sessizce yanlis cevap uretir — asagidaki "kadikoy" vakasi tam olarak
 * o senaryo.
 */
class FollowUpDetectorTest {

    @Test
    void tekBasinaZamanIfadesi() {
        assertThat(FollowUpDetector.isTimeOnly("yarin")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("cuma")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("bugun")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("haftaya")).isTrue();
    }

    @Test
    void baglacliTakipSorusu() {
        assertThat(FollowUpDetector.isTimeOnly("peki yarin")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("ya cuma")).isTrue();
    }

    /**
     * Turkce sondan eklemeli: "yarini", "cumaya", "pazartesiyi". Desen sonda kelime siniriyla
     * kapatilsaydi bu cumlelerin hicbiri takip sayilmazdi.
     */
    @Test
    void cekimEkliZamanIfadeleri() {
        assertThat(FollowUpDetector.isTimeOnly("yarini da soyle")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("cumaya bak")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("pazartesiyi goster")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("yarin icin de bakar misin")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("bana cumayi soyler misin")).isTrue();
    }

    /**
     * Bilesik gun adlari: "cuma" deseni "cumartesi"den once gelseydi geriye "rtesi" kalir ve
     * mesaj takip sayilmazdi.
     */
    @Test
    void bilesikGunAdlari() {
        assertThat(FollowUpDetector.isTimeOnly("cumartesi")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("pazartesi")).isTrue();
    }

    @Test
    void acikTarihDeTakipSorusuOlabilir() {
        assertThat(FollowUpDetector.isTimeOnly("17 agustos")).isTrue();
        assertThat(FollowUpDetector.isTimeOnly("17.08")).isTrue();
    }

    /**
     * ASIL KORUMA: zaman ifadesi olmayan kisa mesajlar baglama KAYMAZ.
     *
     * <pre>
     *   — bugun yemekte ne var?   -> (menu)
     *   — kadikoy                 -> kullanici SERVIS soruyor
     * </pre>
     *
     * Baglam burada devreye girseydi sistem "menu + kadikoy" diye yorumlar ve yanlis cevap
     * donerdi.
     */
    @Test
    void zamanIfadesiOlmayanMesajTakipSayilmaz() {
        assertThat(FollowUpDetector.isTimeOnly("kadikoy")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("muhasebe")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("ayse kaya")).isFalse();
    }

    /**
     * Kendi basina anlamli sorular baglama ihtiyac duymaz; onlar zaten dogru kategoriye
     * gidiyor. Baglam devreye girseydi bir sey degismezdi ama kural genislemis olurdu.
     */
    @Test
    void kendiBasinaAnlamliSorularTakipSayilmaz() {
        assertThat(FollowUpDetector.isTimeOnly("yarin ne var")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("yarin ofiste miyim")).isFalse();
    }

    /**
     * "ne" ve "var" FILLER listesine EKLENMEMELI. Eklenselerdi bu mesaj takip sayilir ve
     * onceki intent calisma_duzeni iken sorulan bir menu sorusu, calisma duzeni olarak
     * yorumlanirdi. Test bu kelimelerin listeye sonradan eklenmesini engelliyor.
     */
    @Test
    void bugunNeVar_takipSayilmaz() {
        assertThat(FollowUpDetector.isTimeOnly("bugun ne var")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("bugun menu ne")).isFalse();
    }

    /** "bu" deseni cikarildi: ek toleransiyla "bunu", "buna" gibi kelimeleri yakalardi. */
    @Test
    void buIleBaslayanKelimelerTakipSayilmaz() {
        assertThat(FollowUpDetector.isTimeOnly("bunu soyle")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("buna bak")).isFalse();
    }

    @Test
    void bosGirdi() {
        assertThat(FollowUpDetector.isTimeOnly(null)).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("")).isFalse();
        assertThat(FollowUpDetector.isTimeOnly("   ")).isFalse();
    }

    /** Baglam yalnizca cevabi ZAMANA bagli intent'lerde anlamli. */
    @Test
    void yalnizcaZamanaBagliIntentlerBaglamTasir() {
        assertThat(FollowUpDetector.isTimeAware("yemek_menusu")).isTrue();
        assertThat(FollowUpDetector.isTimeAware("calisma_duzeni")).isTrue();

        assertThat(FollowUpDetector.isTimeAware("rehber_kisi")).isFalse();
        assertThat(FollowUpDetector.isTimeAware("servis_guzergah")).isFalse();
        assertThat(FollowUpDetector.isTimeAware("intent_bulunamadi")).isFalse();
        assertThat(FollowUpDetector.isTimeAware(null)).isFalse();
    }
}
