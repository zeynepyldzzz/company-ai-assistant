package com.company.assistant.shuttle;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;

/**
 * A-12 (FR-10/11): 'servis_saatleri' ve 'servis_guzergah' intent'leri icin canli servis
 * verisi uretir. MenuVariableResolver deseni: deterministik anahtar kelime eslesmesi + ASCII
 * katlama, LLM yok.
 *
 * Uretilen anahtarlar V24 template'leriyle eslesir:
 *   {{servis_saatleri}}  -> hat basligi + durak/saat listesi
 *   {{servis_guzergahi}} -> hat + plaka + sirali durak listesi
 *
 * Guzergah secimi veri odaklidir: anahtar kelimeler ('kadikoy', 'besiktas'...) koda gomulmez,
 * hat ve durak adlarindan turetilir. Yeni hat eklendiginde kod degismez.
 *
 * Sinir (durum notu): tek hat sorulmadiysa tum hatlar listelenir; serbest dil ve yazim hatasi
 * kapsam disi (Faz 2 / LLM). Veri erisimi ShuttleService uzerindendir; ayni servis Faz 2'de
 * LLM'in "arac"i olur (kopru ilkesi).
 */
@Component
public class ShuttleVariableResolver {

    private static final String HOURS_INTENT = "servis_saatleri";
    private static final String ROUTE_INTENT = "servis_guzergah";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String NO_ROUTE_DATA = "Sistemde tanımlı bir servis güzergahı bulunmuyor.";
    private static final String NO_STOP_DATA = "Bu hat için durak bilgisi girilmemiş görünüyor.";

    /**
     * #127: shuttle_stop tablosunda durak basina tek `time` var; sabah/aksam seferi ayrimi
     * veri modelinde YOK. Kullanici "aksam servisi saat kacta" diye sordugunda sabah
     * saatlerini aksammis gibi sunmamak icin yanitin sonuna bir kez eklenir.
     */
    private static final String PERIOD_NOTE =
            "\n\nNot: Sistemde sabah/akşam seferi ayrımı tanımlı değil; yukarıdakiler kayıtlı kalkış saatleridir.";

    // Hat adlarinda gecen ama ayirt edici olmayan kelimeler; eslestirmede kullanilmaz.
    // Aksi halde "servis saatleri" sorusundaki 'servis' kelimesi her hatti eslestirir.
    private static final Set<String> GENERIC_WORDS = Set.of(
            "servis", "servisi", "hat", "hatti", "yakasi", "iskele", "durak", "duragi");

    private final ShuttleService shuttleService;

    public ShuttleVariableResolver(ShuttleService shuttleService) {
        this.shuttleService = shuttleService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        boolean hours = HOURS_INTENT.equals(intentName);
        boolean route = ROUTE_INTENT.equals(intentName);
        if (!hours && !route) {
            return Map.of();
        }

        String text = TurkishText.foldToAscii(message);
        List<ShuttleRouteResponse> routes = shuttleService.getAllRoutes();
        if (routes.isEmpty()) {
            return Map.of(hours ? "servis_saatleri" : "servis_guzergahi", NO_ROUTE_DATA);
        }

        // #127: duraklar TEK sorguda cekilir. Onceden hat basina getStops cagriliyordu
        // (existsById + durak sorgusu = 2), 9 guzergahli kullanimda mesaj basina 19 sorgu
        // ediyordu. Artik guzergah sayisindan bagimsiz 2 sorgu.
        Map<Integer, List<ShuttleStopResponse>> stopsByRoute = shuttleService.getStopsByRoutes(
                routes.stream().map(ShuttleRouteResponse::getId).toList());
        List<RouteWithStops> all = routes.stream()
                .map(r -> new RouteWithStops(r, stopsByRoute.getOrDefault(r.getId(), List.of())))
                .toList();

        List<RouteWithStops> selected = all.stream().filter(r -> mentions(text, r)).toList();
        if (selected.isEmpty()) {
            // Hat belirtilmemis veya taninmamis -> hepsi. Kullanici hangi hatlar oldugunu
            // gormeden dogru soruyu soramaz; "hangi hat?" diye sormak fazladan tur maliyeti.
            selected = all;
        }

        String body = selected.stream()
                .map(hours ? this::formatHours : this::formatRoute)
                .collect(Collectors.joining("\n\n"));
        // #127: veri modelinde durak basina TEK saat var, sabah/aksam ayrimi yok. "aksam
        // servisi saat kacta" sorusunda sabah saatlerini sessizce sunmamak icin not eklenir.
        if (hours) {
            return Map.of("servis_saatleri", body + PERIOD_NOTE);
        }
        return Map.of("servis_guzergahi", body);
    }

    // Hat adi, durak adlari veya PLAKA mesajda geciyor mu?
    private boolean mentions(String text, RouteWithStops route) {
        if (mentionsPlate(text, route.route().getPlateNumber())) {
            return true;
        }
        List<String> words = new ArrayList<>(keywordsOf(route.route().getName()));
        route.stops().forEach(s -> words.addAll(keywordsOf(s.getName())));
        return words.stream().anyMatch(text::contains);
    }

    /**
     * #124: plaka ile arama. Karsilastirma harf/rakam disindaki her seyi atarak yapilir,
     * boylece "34 SR 101", "34 sr 101" ve "34sr101" ayni hatti bulur.
     */
    private boolean mentionsPlate(String text, String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            return false;
        }
        String plate = compact(TurkishText.foldToAscii(plateNumber));
        return !plate.isBlank() && compact(text).contains(plate);
    }

    private String compact(String value) {
        return value.replaceAll("[^a-z0-9]", "");
    }

    private List<String> keywordsOf(String name) {
        if (name == null) {
            return List.of();
        }
        return List.of(TurkishText.foldToAscii(name).split("[^a-z0-9]+")).stream()
                .filter(w -> w.length() >= 4)
                .filter(w -> !GENERIC_WORDS.contains(w))
                .toList();
    }

    private String formatHours(RouteWithStops route) {
        String header = route.route().getName() + " kalkış saatleri:" + driverLine(route.route());
        if (route.stops().isEmpty()) {
            return header + "\n" + NO_STOP_DATA;
        }
        return header + "\n" + route.stops().stream()
                .map(s -> "• " + formatTime(s) + " " + s.getName())
                .collect(Collectors.joining("\n"));
    }

    private String formatRoute(RouteWithStops route) {
        String header = route.route().getName() + " (plaka: " + route.route().getPlateNumber() + ")"
                + driverLine(route.route());
        if (route.stops().isEmpty()) {
            return header + "\n" + NO_STOP_DATA;
        }
        return header + "\n" + route.stops().stream()
                .map(s -> "• " + s.getName() + " — " + formatTime(s))
                .collect(Collectors.joining("\n"));
    }

    /**
     * A-23 (#148): sofor bilgisi. Servisi kaciran ya da gecikecegini haber vermek isteyen
     * calisanin sofore ulasmasi gerekiyor; veri B-16 ile zaten geldi (shuttle_route.driver_*).
     *
     * <p>Eksik veri SESSIZCE atlanir — sofor atanmamis hatta satir hic basilmaz. Saat/durak
     * alanlarindaki "belirtilmemiş" yaklasimindan farkli davraniyor, cunku orada eksiklik
     * sorunun ta kendisiyle ilgili (kullanici saati soruyor); burada ise sofor satiri ek
     * bilgi, yoklugunu duyurmak gereksiz gurultu olur.
     *
     * <p>Adi olup telefonu olmayan sofor icin yalnizca ad yazilir; hatlar birden fazla
     * listelendiginde her hat kendi soforunu tasir (satir basliga bagli, gövdeye degil).
     */
    private String driverLine(ShuttleRouteResponse route) {
        String name = route.getDriverName();
        if (name == null || name.isBlank()) {
            return "";
        }
        String phone = route.getDriverPhone();
        boolean hasPhone = phone != null && !phone.isBlank();
        return "\nŞoför: " + name + (hasPhone ? " — " + phone : "");
    }

    // Saat girilmemis durakta "null" basmak yerine acik metin.
    private String formatTime(ShuttleStopResponse stop) {
        return stop.getTime() != null ? stop.getTime().format(TIME_FMT) : "saat belirtilmemiş";
    }

    private record RouteWithStops(ShuttleRouteResponse route, List<ShuttleStopResponse> stops) {
    }
}
