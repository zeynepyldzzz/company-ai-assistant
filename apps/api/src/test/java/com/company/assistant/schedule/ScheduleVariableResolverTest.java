package com.company.assistant.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.company.assistant.directory.OfficeStatusVariableResolver;

/**
 * A-13: calisma duzeni resolver'i. Testler hafta gunu anahtar kelimeleriyle yazildi;
 * hedef tarih "bu haftanin Pazartesi'si + n" olarak hesaplandigi icin sonuc, testin
 * kostugu gunden bagimsiz deterministiktir.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleVariableResolverTest {

    @Mock
    private ScheduleService scheduleService;
    @Mock
    private OfficeStatusVariableResolver officeStatusVariableResolver;
    @Mock
    private Authentication authentication;

    private ScheduleVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ScheduleVariableResolver(scheduleService, officeStatusVariableResolver);
        lenient().when(authentication.getName()).thenReturn("7");
    }

    @Test
    void calismaDuzeniDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu", "bugün ne var", authentication)).isEmpty();
    }

    // FR-63: kimlik JWT'den gelir. Kimlik yoksa degisken bos birakilamaz, aksi halde ham
    // {{calisma_duzenim}} kullaniciya gorunur.
    @Test
    void kimlikYoksaAcikMesajDonerPlaceholderSizmaz() {
        Map<String, String> vars = resolver.resolve("calisma_duzeni", "bugün ofiste miyim", null);

        assertThat(vars.get("calisma_duzenim")).contains("giriş yapmış olman gerekiyor");
    }

    @Test
    void kayitYoksaNetMesajDoner() {
        when(scheduleService.getMySchedule(anyInt()))
                .thenReturn(new WeeklyScheduleDto(weekStart(), List.of()));

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "bu hafta ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim")).contains("kayıtlı bir çalışma düzenin görünmüyor");
    }

    @Test
    void tekGunSorusuOGununDurumunuDoner() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "çarşamba ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim"))
                .contains("Çarşamba")
                .contains("uzaktan çalışıyorsun");
    }

    @Test
    void haftaSorusuTumGunleriListeler() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "bu hafta çalışma düzenim nedir", authentication);

        assertThat(vars.get("calisma_duzenim"))
                .contains("Pazartesi — Ofis")
                .contains("Çarşamba — Uzaktan")
                .contains("Cuma — İzinli");
    }

    // ScheduleService yalnizca icinde bulunulan haftayi verir; sessizce bu haftayi dondurmek
    // yanlis cevap olur.
    @Test
    void gelecekHaftaSorulursaKapsamDisiMesajiDoner() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "gelecek hafta ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim")).contains("yalnızca içinde bulunduğumuz haftanın");
    }

    @Test
    void gelecekHaftaninBelirliGunuDeKapsamDisi() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "gelecek hafta çarşamba ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim")).contains("yalnızca içinde bulunduğumuz haftanın");
    }

    @Test
    void haftaSonuSorulursaAcikMesajDoner() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "cumartesi ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim")).contains("Pazartesi-Cuma");
    }

    @Test
    void kaydiOlmayanGunIcinNetMesajDoner() {
        when(scheduleService.getMySchedule(anyInt())).thenReturn(new WeeklyScheduleDto(weekStart(),
                List.of(new ScheduleDayDto("monday", ScheduleStatus.OFFICE))));

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "perşembe ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim"))
                .contains("Perşembe")
                .contains("kayıtlı bir durum yok");
    }

    @Test
    void turkceKaraktersizGirisDeEslesir() {
        seedFullWeek();

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "persembe ofiste miyim", authentication);

        assertThat(vars.get("calisma_duzenim"))
                .contains("Perşembe")
                .contains("ofistesin");
    }

    // A-14 (#115): ucuncu sahis sorusu rehber kaynagina delege edilir; haftalik plan hic
    // okunmaz (kullanicinin kendi plani olmasa bile yanit gelmeli).
    @Test
    void ucuncuSahisSorusuRehberKaynaginaDelegeEdilir() {
        when(officeStatusVariableResolver.isThirdPersonQuestion("kimler ofiste")).thenReturn(true);
        when(officeStatusVariableResolver.resolve("kimler ofiste", 7))
                .thenReturn("Bilgi Teknolojileri departmanında ofiste görünenler:\n• Ayse Kaya");

        Map<String, String> vars = resolver.resolve("calisma_duzeni", "kimler ofiste", authentication);

        assertThat(vars.get("calisma_duzenim")).contains("Ayse Kaya");
        verifyNoInteractions(scheduleService);
    }

    private LocalDate weekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private void seedFullWeek() {
        when(scheduleService.getMySchedule(anyInt())).thenReturn(new WeeklyScheduleDto(weekStart(), List.of(
                new ScheduleDayDto("monday", ScheduleStatus.OFFICE),
                new ScheduleDayDto("tuesday", ScheduleStatus.OFFICE),
                new ScheduleDayDto("wednesday", ScheduleStatus.REMOTE),
                new ScheduleDayDto("thursday", ScheduleStatus.OFFICE),
                new ScheduleDayDto("friday", ScheduleStatus.LEAVE))));
    }
}
