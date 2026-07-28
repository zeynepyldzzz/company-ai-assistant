package com.company.assistant.shuttle;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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

        String text = foldToAscii(message);
        List<ShuttleRouteResponse> routes = shuttleService.getAllRoutes();
        if (routes.isEmpty()) {
            return Map.of(hours ? "servis_saatleri" : "servis_guzergahi", NO_ROUTE_DATA);
        }

        // Her hat icin duraklari bir kez cekilir; hem eslestirmede (durak adi ipucu olabilir)
        // hem de ciktida kullanilir.
        List<RouteWithStops> all = routes.stream()
                .map(r -> new RouteWithStops(r, shuttleService.getStops(r.getId())))
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
        return Map.of(hours ? "servis_saatleri" : "servis_guzergahi", body);
    }

    // Hat adi veya durak adlarindaki ayirt edici kelimelerden biri mesajda geciyor mu?
    private boolean mentions(String text, RouteWithStops route) {
        List<String> words = new ArrayList<>(keywordsOf(route.route().getName()));
        route.stops().forEach(s -> words.addAll(keywordsOf(s.getName())));
        return words.stream().anyMatch(text::contains);
    }

    private List<String> keywordsOf(String name) {
        if (name == null) {
            return List.of();
        }
        return List.of(foldToAscii(name).split("[^a-z0-9]+")).stream()
                .filter(w -> w.length() >= 4)
                .filter(w -> !GENERIC_WORDS.contains(w))
                .toList();
    }

    private String formatHours(RouteWithStops route) {
        String header = route.route().getName() + " kalkış saatleri:";
        if (route.stops().isEmpty()) {
            return header + "\n" + NO_STOP_DATA;
        }
        return header + "\n" + route.stops().stream()
                .map(s -> "• " + formatTime(s) + " " + s.getName())
                .collect(Collectors.joining("\n"));
    }

    private String formatRoute(RouteWithStops route) {
        String header = route.route().getName() + " (plaka: " + route.route().getPlateNumber() + ")";
        if (route.stops().isEmpty()) {
            return header + "\n" + NO_STOP_DATA;
        }
        return header + "\n" + route.stops().stream()
                .map(s -> "• " + s.getName() + " — " + formatTime(s))
                .collect(Collectors.joining("\n"));
    }

    // Saat girilmemis durakta "null" basmak yerine acik metin.
    private String formatTime(ShuttleStopResponse stop) {
        return stop.getTime() != null ? stop.getTime().format(TIME_FMT) : "saat belirtilmemiş";
    }

    private String foldToAscii(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(new Locale("tr"))
                .replace('ç', 'c')
                .replace('ş', 's')
                .replace('ı', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ö', 'o');
    }

    private record RouteWithStops(ShuttleRouteResponse route, List<ShuttleStopResponse> stops) {
    }
}
