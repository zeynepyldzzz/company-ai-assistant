package com.company.assistant.shuttle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-12: servis intent'lerinde canli veri uretimi. Odak, issue'nun is kurallari:
 * hat secimi, hat belirtilmediginde tum hatlar, veri yoklugu ve Turkce karaktersiz giris.
 */
@ExtendWith(MockitoExtension.class)
class ShuttleVariableResolverTest {

    @Mock
    private ShuttleService shuttleService;

    private ShuttleVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ShuttleVariableResolver(shuttleService);
    }

    @Test
    void servisDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu", "bugün ne var")).isEmpty();
    }

    @Test
    void hatAdiGecinceSadeceOHatDoner() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "kadıköy servisi kaçta kalkıyor");

        assertThat(vars.get("servis_saatleri"))
                .contains("Kadikoy Hatti")
                .contains("07:00 Kadikoy Iskele")
                .doesNotContain("Besiktas");
    }

    @Test
    void durakAdiGecinceDeOHatDoner() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "mecidiyeköy durağına servis geliyor mu");

        assertThat(vars.get("servis_saatleri"))
                .contains("Besiktas Hatti")
                .doesNotContain("Kadikoy");
    }

    @Test
    void hatBelirtilmezseTumHatlarListelenir() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis saatleri nedir");

        assertThat(vars.get("servis_saatleri"))
                .contains("Kadikoy Hatti")
                .contains("Besiktas Hatti");
    }

    @Test
    void guzergahIntentindePlakaVeDuraklarDoner() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_guzergah", "kadıköy servisi nereden geçiyor");

        assertThat(vars).containsOnlyKeys("servis_guzergahi");
        assertThat(vars.get("servis_guzergahi"))
                .contains("plaka: 34 SR 101")
                .contains("Kadikoy Iskele — 07:00")
                .contains("Bostanci — 07:20");
    }

    @Test
    void hicGuzergahYoksaNetMesajDoner() {
        when(shuttleService.getAllRoutes()).thenReturn(List.of());

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri")).isEqualTo("Sistemde tanımlı bir servis güzergahı bulunmuyor.");
    }

    @Test
    void duragiOlmayanHatIcinNetMesajDoner() {
        when(shuttleService.getAllRoutes()).thenReturn(List.of(route(1, "Yeni Hat", "34 XX 001")));
        when(shuttleService.getStops(1)).thenReturn(List.of());

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri")).contains("Bu hat için durak bilgisi girilmemiş görünüyor.");
    }

    // Saat NULL ise template'e "null" sizmamali (bilinmeyen degisken kullaniciya gorunur kuralı).
    @Test
    void saatiOlmayanDurakAcikMetinBasar() {
        when(shuttleService.getAllRoutes()).thenReturn(List.of(route(1, "Yeni Hat", "34 XX 001")));
        when(shuttleService.getStops(1)).thenReturn(List.of(stop("Merkez", null, 1)));

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri"))
                .contains("saat belirtilmemiş")
                .doesNotContain("null");
    }

    @Test
    void turkceKaraktersizGirisDeEslesir() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "besiktas servisi kacta kalkiyor");

        assertThat(vars.get("servis_saatleri"))
                .contains("Besiktas Hatti")
                .doesNotContain("Kadikoy");
    }

    private void seedTwoRoutes() {
        lenient().when(shuttleService.getAllRoutes()).thenReturn(List.of(
                route(1, "Anadolu Yakasi - Kadikoy Hatti", "34 SR 101"),
                route(2, "Avrupa Yakasi - Besiktas Hatti", "34 SR 202")));
        lenient().when(shuttleService.getStops(1)).thenReturn(List.of(
                stop("Kadikoy Iskele", LocalTime.of(7, 0), 1),
                stop("Bostanci", LocalTime.of(7, 20), 2)));
        lenient().when(shuttleService.getStops(2)).thenReturn(List.of(
                stop("Besiktas Iskele", LocalTime.of(7, 15), 1),
                stop("Mecidiyekoy", LocalTime.of(7, 35), 2)));
    }

    private ShuttleRouteResponse route(Integer id, String name, String plate) {
        ShuttleRoute entity = new ShuttleRoute();
        entity.setId(id);
        entity.setName(name);
        entity.setPlateNumber(plate);
        return new ShuttleRouteResponse(entity);
    }

    private ShuttleStopResponse stop(String name, LocalTime time, int orderIndex) {
        ShuttleStop entity = new ShuttleStop();
        entity.setName(name);
        entity.setTime(time);
        entity.setOrderIndex(orderIndex);
        return new ShuttleStopResponse(entity);
    }
}
