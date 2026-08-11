package com.company.assistant.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-40 (#209): durum sorgularinda gun kapsami karari.
 *
 * <p>Referans gun PAZARTESI sabitlendi: "carsamba" sorgusu bugunden AYRISMALI, yoksa test
 * yanlis gunun donduruldugunu yakalayamaz. Takvim bagimliligi {@code TodayStatusService}
 * mock'uyla kesiliyor — aksi halde Cumartesi kosan CI'da her test hafta sonu dalina duserdi.
 */
@ExtendWith(MockitoExtension.class)
class StatusDayResolverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 1).with(DayOfWeek.MONDAY);

    @Mock
    private TodayStatusService todayStatusService;

    private StatusDayResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StatusDayResolver(todayStatusService);
        lenient().when(todayStatusService.today()).thenReturn(TODAY);
        lenient().when(todayStatusService.currentWeekStart()).thenReturn(TODAY.with(DayOfWeek.MONDAY));
    }

    // Gun belirtilmemis: bugun DOGRU cevap, o yuzden onek de yok (yanit metni degismesin).
    @Test
    void gunBelirtilmemisseBugunVeOneksizDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("kimler ofiste");

        assertThat(scope.failed()).isFalse();
        assertThat(scope.dayKey()).isEqualTo("monday");
        assertThat(scope.prefix()).isEmpty();
    }

    // Gun ACIKCA soruldu: yanit hangi gunu gosterdigini soylemeli.
    @Test
    void haftaGunuSorulursaOGunVeOnekDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("carsamba kimler ofiste");

        assertThat(scope.dayKey()).isEqualTo("wednesday");
        assertThat(scope.prefix()).contains("Çarşamba");
    }

    /**
     * Tarih SAYISAL bicimde uretiliyor, ay adiyla degil: {@code TODAY} icinde bulunulan ISO
     * haftasinin pazartesisi ve o hafta ay sinirina denk gelebilir (agustosun ilki bir hafta
     * ortasiysa pazartesi temmuzda kalir). Sabit bir ay adi yazmak testi yilda birkac kez
     * yanlis sebepten kirardi.
     */
    @Test
    void acikTarihDeCozulur() {
        LocalDate wednesday = TODAY.with(DayOfWeek.WEDNESDAY);

        StatusDayResolver.DayScope scope = resolver.resolve(
                wednesday.format(DateTimeFormatter.ofPattern("dd.MM")) + " kimler ofiste");

        assertThat(scope.dayKey()).isEqualTo("wednesday");
        assertThat(scope.prefix()).contains("Çarşamba");
    }

    // A-37 deseni: tarih ifadesi VAR ama cozulemedi -> bugune DUSULMEZ.
    @Test
    void cozulemeyenTarihHataDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("agustos kimler ofiste");

        assertThat(scope.failed()).isTrue();
        assertThat(scope.message()).contains("Hangi günü sorduğunu");
        assertThat(scope.dayKey()).isNull();
    }

    @Test
    void haftaSonuHataDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("cumartesi kimler ofiste");

        assertThat(scope.failed()).isTrue();
        assertThat(scope.message()).contains("hafta sonuna denk geliyor");
    }

    /**
     * Hafta disi gun kapsam disi mesaji doner.
     *
     * <p>Ayni test KRITIK bir siralamayi da sabitliyor: "haftaya çarşamba" icinde "hafta"
     * kelimesi geciyor ama TEKIL gun ipucu var, dolayisiyla hafta kapsami kontrolu bunu
     * EZMEMELI. Siralama ters olsaydi kullanici belirli bir gun sorarken "haftalık görünüm
     * veremiyorum" gibi alakasiz bir mesaj alirdi.
     */
    @Test
    void haftaDisiGunKapsamDisiDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("haftaya carsamba kimler ofiste");

        assertThat(scope.failed()).isTrue();
        assertThat(scope.message())
                .contains("yalnızca içinde bulunduğumuz haftanın")
                .doesNotContain("Haftalık toplu durum");
    }

    /**
     * A-40 Sorun 6: "bu hafta kimler ofiste" sessizce BUGUNU donduruyordu ve gun ipucu
     * bulunamadigi icin yanitta etiket de yoktu — kullanicinin fark etme sansi sifirdi.
     */
    @Test
    void haftaKapsamiAcikMesajDoner() {
        StatusDayResolver.DayScope scope = resolver.resolve("bu hafta kimler ofiste");

        assertThat(scope.failed()).isTrue();
        assertThat(scope.message()).contains("Haftalık toplu durum görünümü veremiyorum");
    }

}
