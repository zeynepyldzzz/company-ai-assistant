package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.common.PagedResponse;

/**
 * A-39 (#212): mevcut sayimi soran sorular. Odak, issue'nun is kurallari — hangi sayinin
 * dondugu ve DURUM sayimiyla karismamasi.
 */
@ExtendWith(MockitoExtension.class)
class CountVariableResolverTest {

    private static final String VARIABLE = "sayim_bilgisi";

    @Mock
    private DirectoryService directoryService;
    @Mock
    private DepartmentService departmentService;

    private CountVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CountVariableResolver(directoryService, departmentService);
    }

    /**
     * Stub'lar BILEREK setUp'ta degil: {@code sayimDisiIntentBosDoner} testi
     * {@code verifyNoInteractions} kullaniyor ve mock'a hic dokunulmamis olmasi gerekiyor.
     * Her test neye ihtiyaci varsa onu kuruyor.
     */
    private void seedCounts() {
        lenient().when(directoryService.searchEmployees(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 1, 13));
        lenient().when(departmentService.getDepartmentNames())
                .thenReturn(List.of("Muhasebe ve Finans", "Bilgi Teknolojileri",
                        "Insan Kaynaklari", "Satis ve Pazarlama"));
    }

    @Test
    void sayimDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu", "toplam kaç çalışan var")).isEmpty();

        verifyNoInteractions(directoryService, departmentService);
    }

    @Test
    void calisanSayisiSorulursaYalnizcaOSayiDoner() {
        seedCounts();

        String reply = resolver.resolve("sayim", "toplam kaç çalışan var").get(VARIABLE);

        assertThat(reply).isEqualTo("Şirkette 13 aktif çalışan var.");
    }

    @Test
    void departmanSayisiSorulursaYalnizcaOSayiDoner() {
        seedCounts();

        String reply = resolver.resolve("sayim", "kaç departman var").get(VARIABLE);

        assertThat(reply).isEqualTo("Şirkette 4 departman var.");
    }

    /**
     * Belirsiz sayim sorusunda rastgele birini secmek yerine ikisi de veriliyor; kullanici
     * hangisini istediyse onu okur. Yanlis sayiyi tek basina dondurmek, iki sayiyi birden
     * vermekten kotudur.
     */
    @Test
    void ipucuYoksaIkiSayiDaDoner() {
        seedCounts();

        String reply = resolver.resolve("sayim", "kaç tane var").get(VARIABLE);

        assertThat(reply).isEqualTo("Şirkette 13 aktif çalışan ve 4 departman var.");
    }

    // "personel" ve "kadro" da calisan ipucu; tek kelimeye bagli kalinmamali.
    @Test
    void esanlamliCalisanKelimeleriDeTaninir() {
        seedCounts();

        assertThat(resolver.resolve("sayim", "kaç personel çalışıyor").get(VARIABLE))
                .contains("aktif çalışan")
                .doesNotContain("departman");
    }

    /**
     * KRITIK: sayim sorgusu DURUM filtresi uygulamaz ve satirlari cekmez.
     *
     * <p>Ucuncu parametre {@code office}; null gecmezse "toplam kaç çalışan var" sorusuna
     * ofistekilerin sayisi donerdi — A-38 sonrasi ortaya cikan hatanin ta kendisi. Sayfa
     * boyutu 1: yalnizca {@code total} okunuyor.
     */
    @Test
    void durumFiltresiUygulanmazVeSatirCekilmez() {
        seedCounts();

        resolver.resolve("sayim", "toplam kaç çalışan var");

        verify(directoryService).searchEmployees(isNull(), isNull(), isNull(), eq(0), eq(1));
    }
}
