package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.directory.DepartmentService;
import com.company.assistant.directory.DirectoryService;
import com.company.assistant.shuttle.ShuttleRoute;
import com.company.assistant.shuttle.ShuttleRouteResponse;
import com.company.assistant.shuttle.ShuttleService;
import com.company.assistant.shuttle.ShuttleStop;
import com.company.assistant.shuttle.ShuttleStopResponse;

/**
 * A-19 (#129): varlik farkindalikli kural katmani. Odak: alan kelimesi + varlik adi
 * birlikteligi, ve alan kelimesi yokken DB'ye hic gidilmemesi.
 */
@ExtendWith(MockitoExtension.class)
class RuleBasedIntentMatcherTest {

    @Mock
    private ShuttleService shuttleService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private DepartmentService departmentService;

    private RuleBasedIntentMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new RuleBasedIntentMatcher(shuttleService, directoryService, departmentService);
    }

    @Test
    void plakaGuzergahIntentineGider() {
        var result = matcher.match("34 SR 101");

        assertThat(result).isPresent();
        assertThat(result.get().intent()).isEqualTo("servis_guzergah");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] plaka");
    }

    // Olculdu: "kadıköy servisi" ~ ornek "bostancı servisi" -> 0.597. Ozel isim baskin
    // oldugu icin embedding cozemiyordu.
    @Test
    void durakAdiVeServisKelimesiGuzergahIntentineGider() {
        seedShuttle();

        var result = matcher.match("kadıköy servisi");

        assertThat(result).isPresent();
        assertThat(result.get().intent()).isEqualTo("servis_guzergah");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] durak/hat adı");
    }

    @Test
    void saatSorusuVarsaSaatIntentineGider() {
        seedShuttle();

        assertThat(matcher.match("kadıköy servisi kaçta").get().intent()).isEqualTo("servis_saatleri");
    }

    // Varlik yok ama bu uygulamada "servis" + saat sorusu personel servisinden baska bir sey
    // ifade etmiyor. Olculdu: "çarşamba servisi kaçta" 0.671 ile esik altinda kaliyordu.
    @Test
    void varlikYoksaDaServisArtiSaatSaatIntentineGider() {
        seedShuttle();

        var result = matcher.match("çarşamba servisi kaçta");

        assertThat(result.get().intent()).isEqualTo("servis_saatleri");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] servis + saat");
    }

    @Test
    void departmanAdiVeBolumKelimesiDepartmanIntentineGider() {
        seedDepartments();

        var result = matcher.match("muhasebe bölümü");

        assertThat(result.get().intent()).isEqualTo("rehber_departman");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] departman adı");
    }

    @Test
    void calisanAdiVeDahiliKelimesiKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        var result = matcher.match("Ayşe Kaya'nın dahilisi kaç");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] çalışan adı");
    }

    // Iki asamali tetikleme: alan kelimesi yoksa DB'ye HIC gidilmez. "Can" ve "Deniz" gercek
    // calisan adlari; alan kelimesi olmadan isim eslestirme calissaydi "canım sıkıldı"
    // rehber_kisi'ye giderdi.
    @Test
    void alanKelimesiYoksaVeriTabaninaGidilmez() {
        assertThat(matcher.match("canım sıkıldı")).isEmpty();
        assertThat(matcher.match("bugün yemekte ne var")).isEmpty();

        verifyNoInteractions(shuttleService, directoryService, departmentService);
    }

    // Alan kelimesi var ama varlik yok -> kural devreye girmez, embedding yoluna kalir.
    @Test
    void alanKelimesiVarVarlikYoksaKuralEslesmez() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(false);

        assertThat(matcher.match("telefon numarası nasıl bulunur")).isEmpty();
    }

    private void seedShuttle() {
        lenient().when(shuttleService.getAllRoutes())
                .thenReturn(List.of(route(1, "Anadolu Yakasi - Kadikoy Hatti", "34 SR 101")));
        lenient().when(shuttleService.getStopsByRoutes(anyCollection()))
                .thenReturn(Map.of(1, List.of(stop("Kadikoy Iskele", LocalTime.of(7, 0)))));
    }

    private void seedDepartments() {
        when(departmentService.getDepartmentNames())
                .thenReturn(List.of("Muhasebe ve Finans", "Bilgi Teknolojileri"));
    }

    private ShuttleRouteResponse route(Integer id, String name, String plate) {
        ShuttleRoute entity = new ShuttleRoute();
        entity.setId(id);
        entity.setName(name);
        entity.setPlateNumber(plate);
        return new ShuttleRouteResponse(entity);
    }

    private ShuttleStopResponse stop(String name, LocalTime time) {
        ShuttleStop entity = new ShuttleStop();
        entity.setName(name);
        entity.setTime(time);
        entity.setOrderIndex(1);
        return new ShuttleStopResponse(entity);
    }
}
