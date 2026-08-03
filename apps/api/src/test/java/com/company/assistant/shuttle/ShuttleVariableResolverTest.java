package com.company.assistant.shuttle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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

    // #124: plaka ile arama. Bosluk ve buyuk-kucuk harf farklari eslesmenin onune gecmemeli.
    @Test
    void plakaIleHatBulunur() {
        seedTwoRoutes();

        assertThat(resolver.resolve("servis_guzergah", "34 SR 101 hangi hat").get("servis_guzergahi"))
                .contains("Kadikoy Hatti").doesNotContain("Besiktas");
        assertThat(resolver.resolve("servis_guzergah", "34sr202 nereden geciyor").get("servis_guzergahi"))
                .contains("Besiktas Hatti").doesNotContain("Kadikoy");
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
        when(shuttleService.getStopsByRoutes(anyCollection())).thenReturn(Map.of());

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri")).contains("Bu hat için durak bilgisi girilmemiş görünüyor.");
    }

    // Saat NULL ise template'e "null" sizmamali (bilinmeyen degisken kullaniciya gorunur kuralı).
    @Test
    void saatiOlmayanDurakAcikMetinBasar() {
        when(shuttleService.getAllRoutes()).thenReturn(List.of(route(1, "Yeni Hat", "34 XX 001")));
        when(shuttleService.getStopsByRoutes(anyCollection()))
                .thenReturn(Map.of(1, List.of(stop("Merkez", null, 1))));

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

    // #127: duraklar guzergah sayisindan bagimsiz TEK toplu sorguyla cekilmeli.
    @Test
    void duraklarTekSorgudaCekilir() {
        seedTwoRoutes();

        resolver.resolve("servis_saatleri", "servis saatleri nedir");

        verify(shuttleService).getStopsByRoutes(anyCollection());
        verify(shuttleService, org.mockito.Mockito.never()).getStops(org.mockito.ArgumentMatchers.anyInt());
    }

    // #127: veri modelinde sabah/aksam ayrimi yok; saat yanitinda bu acikca belirtilmeli,
    // aksi halde "aksam servisi kacta" sorusuna sabah saatleri aksammis gibi donuyor.
    @Test
    void saatYanitindaSeferAyrimiOlmadigiBelirtilir() {
        seedTwoRoutes();

        String hours = resolver.resolve("servis_saatleri", "akşam servisi saat kaçta").get("servis_saatleri");
        String route = resolver.resolve("servis_guzergah", "servis nereden geçiyor").get("servis_guzergahi");

        assertThat(hours).contains("sabah/akşam seferi ayrımı tanımlı değil");
        // Guzergah yanitinda saat listesi asil konu degil; not tekrarlanmaz.
        assertThat(route).doesNotContain("sabah/akşam seferi ayrımı");
    }

    // --- A-23 (#148): sofor bilgisi ---

    @Test
    void saatYanitindaSoforBilgisiGorunur() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "kadıköy servisi kaçta");

        assertThat(vars.get("servis_saatleri")).contains("Şoför: Ahmet Yilmaz — 0532 111 22 33");
    }

    @Test
    void guzergahYanitindaSoforBilgisiGorunur() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_guzergah", "kadıköy servisi nereden geçiyor");

        assertThat(vars.get("servis_guzergahi")).contains("Şoför: Ahmet Yilmaz — 0532 111 22 33");
    }

    // Sofor atanmamis hatta satir HIC basilmaz: eksikligi duyurmak gereksiz gurultu.
    @Test
    void soforuOlmayanHattaSatirBasilmaz() {
        when(shuttleService.getAllRoutes()).thenReturn(List.of(route(1, "Yeni Hat", "34 XX 001")));
        when(shuttleService.getStopsByRoutes(anyCollection()))
                .thenReturn(Map.of(1, List.of(stop("Merkez", LocalTime.of(8, 0), 1))));

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri"))
                .doesNotContain("Şoför")
                .doesNotContain("null");
    }

    @Test
    void telefonuOlmayanSofordeYalnizcaAdBasilir() {
        when(shuttleService.getAllRoutes())
                .thenReturn(List.of(routeWithDriver(1, "Yeni Hat", "34 XX 001", "Veli Kaya", null)));
        when(shuttleService.getStopsByRoutes(anyCollection()))
                .thenReturn(Map.of(1, List.of(stop("Merkez", LocalTime.of(8, 0), 1))));

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis kaçta");

        assertThat(vars.get("servis_saatleri"))
                .contains("Şoför: Veli Kaya")
                .doesNotContain("—")
                .doesNotContain("null");
    }

    // Tum hatlar listelendiginde her hat KENDI soforunu tasimali; satir basliga bagli.
    @Test
    void tumHatlarListelendigindeHerHatKendiSoforunuGosterir() {
        seedTwoRoutes();

        Map<String, String> vars = resolver.resolve("servis_saatleri", "servis saatleri nedir");

        assertThat(vars.get("servis_saatleri"))
                .contains("Şoför: Ahmet Yilmaz")
                .contains("Şoför: Mehmet Ozturk");
    }

    private void seedTwoRoutes() {
        lenient().when(shuttleService.getAllRoutes()).thenReturn(List.of(
                routeWithDriver(1, "Anadolu Yakasi - Kadikoy Hatti", "34 SR 101",
                        "Ahmet Yilmaz", "0532 111 22 33"),
                routeWithDriver(2, "Avrupa Yakasi - Besiktas Hatti", "34 SR 202",
                        "Mehmet Ozturk", "0533 444 55 66")));
        // #127: duraklar artik hat basina degil, tek toplu sorguyla cekiliyor.
        lenient().when(shuttleService.getStopsByRoutes(anyCollection())).thenReturn(Map.of(
                1, List.of(
                        stop("Kadikoy Iskele", LocalTime.of(7, 0), 1),
                        stop("Bostanci", LocalTime.of(7, 20), 2)),
                2, List.of(
                        stop("Besiktas Iskele", LocalTime.of(7, 15), 1),
                        stop("Mecidiyekoy", LocalTime.of(7, 35), 2))));
    }

    /** Sofor bilgisi olmayan hat — A-23 oncesi davranisi da temsil eder. */
    private ShuttleRouteResponse route(Integer id, String name, String plate) {
        return routeWithDriver(id, name, plate, null, null);
    }

    private ShuttleRouteResponse routeWithDriver(Integer id, String name, String plate,
                                                 String driverName, String driverPhone) {
        ShuttleRoute entity = new ShuttleRoute();
        entity.setId(id);
        entity.setName(name);
        entity.setPlateNumber(plate);
        entity.setDriverName(driverName);
        entity.setDriverPhone(driverPhone);
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
