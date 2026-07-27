package com.company.assistant.menu;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * A-11: 'yemek_menusu' intent'i icin canli menu degiskenlerini uretir.
 * HrProcedureVariableResolver deseni; ek olarak kullanicinin ham mesajindan gunu/haftayi
 * cikarir (deterministik kelime + tarih matematigi, LLM yok).
 *
 * Uretilen anahtarlar V22 template'iyle eslesir:
 *   {{menu_gunu}}    -> baslik satiri, or. "Çarşamba (29.07.2026) menüsü:" / "Bu haftanın menüsü:"
 *   {{gunun_menusu}} -> madde madde yemek listesi (veya "menu girilmemis" metni)
 *
 * Sinir (durum notu): sadece tanimli kelime kaliplari (bugun/yarin/obur gun/hafta gunleri
 * + "gelecek hafta" oneki + "hafta" -> tum hafta). Serbest dil ifadeleri Faz 2 (LLM) isidir.
 * resolveTarget() parcasi ileride LLM tabanli cikarimla degistirilebilir; veri/format kismi
 * (MenuService + formatItems) aynen kalir (kopru ilkesi).
 */
@Component
public class MenuVariableResolver {

    private static final String MENU_INTENT = "yemek_menusu";
    private static final String NO_MENU = "Bu tarih için menü girilmemiş görünüyor.";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    private static final Map<DayOfWeek, String> TR_DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Pazartesi",
            DayOfWeek.TUESDAY, "Salı",
            DayOfWeek.WEDNESDAY, "Çarşamba",
            DayOfWeek.THURSDAY, "Perşembe",
            DayOfWeek.FRIDAY, "Cuma",
            DayOfWeek.SATURDAY, "Cumartesi",
            DayOfWeek.SUNDAY, "Pazar");

    // Bilesik gunler (pazartesi/cumartesi) once gelmeli: "pazar" "pazartesi"nin,
    // "cuma" "cumartesi"nin alt-dizesi. Bu sirayla kontrol edilir.
    private static final List<Map.Entry<String, DayOfWeek>> WEEKDAY_KEYWORDS = List.of(
            Map.entry("pazartesi", DayOfWeek.MONDAY),
            Map.entry("sali", DayOfWeek.TUESDAY),
            Map.entry("carsamba", DayOfWeek.WEDNESDAY),
            Map.entry("persembe", DayOfWeek.THURSDAY),
            Map.entry("cumartesi", DayOfWeek.SATURDAY),
            Map.entry("cuma", DayOfWeek.FRIDAY),
            Map.entry("pazar", DayOfWeek.SUNDAY));

    private final MenuService menuService;

    public MenuVariableResolver(MenuService menuService) {
        this.menuService = menuService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!MENU_INTENT.equals(intentName)) {
            return Map.of();
        }

        String text = foldToAscii(message);
        Map<String, String> variables = new HashMap<>();

        // Hafta modu: "hafta" var ve tekil gun ipucu yok (or. "bu haftanin listesi").
        // "gelecek hafta cuma" gibi tekil gun iceren istekler asagidaki tek-gun dalina gider.
        if (text.contains("hafta") && !hasSingleDayCue(text)) {
            List<MenuResponse> week = menuService.getWeeklyMenu();
            if (week.isEmpty()) {
                // Menu yokken "Bu haftanin menusu:" basligi yaniltici olur; net cumle.
                variables.put("menu_gunu", "Bu hafta için menü bulunmuyor.");
                variables.put("gunun_menusu", "");
            } else {
                variables.put("menu_gunu", "Bu haftanın menüsü:");
                variables.put("gunun_menusu", buildWeekBody(week));
            }
            return variables;
        }

        LocalDate target = resolveTarget(text, LocalDate.now());
        String dayLabel = TR_DAY_NAMES.get(target.getDayOfWeek()) + " (" + target.format(DATE_FMT) + ")";
        Optional<MenuResponse> menu = menuService.getMenuByDate(target);
        if (menu.isPresent() && menu.get().getItems() != null && !menu.get().getItems().isEmpty()) {
            variables.put("menu_gunu", dayLabel + " menüsü:");
            variables.put("gunun_menusu", formatItems(menu.get()));
        } else {
            // Menu yoksa "... menusu:" basligi basmak yaniltici (issue #104); tek net cumle.
            variables.put("menu_gunu", dayLabel + " için menü bulunmuyor.");
            variables.put("gunun_menusu", "");
        }
        return variables;
    }

    private boolean hasSingleDayCue(String text) {
        if (text.contains("bugun") || text.contains("yarin") || text.contains("obur gun")) {
            return true;
        }
        return WEEKDAY_KEYWORDS.stream().anyMatch(e -> text.contains(e.getKey()));
    }

    // Ham (ASCII'ye katlanmis) mesajdan hedef tarihi cikarir.
    private LocalDate resolveTarget(String text, LocalDate today) {
        if (text.contains("bugun")) {
            return today;
        }
        if (text.contains("yarin")) {
            return today.plusDays(1);
        }
        if (text.contains("obur gun")) {
            return today.plusDays(2);
        }

        boolean nextWeek = text.contains("gelecek") || text.contains("onumuzdeki")
                || text.contains("haftaya");
        for (Map.Entry<String, DayOfWeek> entry : WEEKDAY_KEYWORDS) {
            if (text.contains(entry.getKey())) {
                LocalDate day = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusDays(entry.getValue().getValue() - 1L);
                return nextWeek ? day.plusDays(7) : day;
            }
        }

        // Gun belirtilmemis/taninmamis -> bugun.
        return today;
    }

    // Bu haftanin tum gunlerini tarihe gore siralayip her gunu baslikli bloklar halinde basar.
    // Cagiran bos hafta durumunu zaten ele aliyor; burada liste dolu gelir.
    private String buildWeekBody(List<MenuResponse> week) {
        return week.stream()
                .sorted(Comparator.comparing(MenuResponse::getDate))
                .map(m -> TR_DAY_NAMES.get(m.getDate().getDayOfWeek())
                        + " (" + m.getDate().format(DATE_FMT) + ")\n" + formatItems(m))
                .collect(Collectors.joining("\n\n"));
    }

    private String formatItems(MenuResponse menu) {
        if (menu.getItems() == null || menu.getItems().isEmpty()) {
            return NO_MENU;
        }
        return menu.getItems().stream()
                .map(item -> item.getCalories() != null
                        ? "• " + item.getName() + " (" + item.getCalories() + " kcal)"
                        : "• " + item.getName())
                .collect(Collectors.joining("\n"));
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
}
