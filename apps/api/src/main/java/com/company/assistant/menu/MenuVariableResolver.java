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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;

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

    // "N gun sonra" — rakam formu. Yazi formu ayri haritada.
    private static final Pattern DAYS_LATER = Pattern.compile("(\\d+)\\s*gun\\s+sonra");
    private static final Map<String, Integer> WORD_NUMBERS = Map.of(
            "bir", 1, "iki", 2, "uc", 3, "dort", 4, "bes", 5, "alti", 6, "yedi", 7);

    private final MenuService menuService;

    public MenuVariableResolver(MenuService menuService) {
        this.menuService = menuService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!MENU_INTENT.equals(intentName)) {
            return Map.of();
        }

        String text = TurkishText.foldToAscii(message);
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
        String dayLabel = TurkishText.dayName(target.getDayOfWeek()) + " (" + target.format(DATE_FMT) + ")";
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

    // "2 gun sonra" / "iki gun sonra" -> 2. Bulamazsa null.
    private Integer extractDaysLater(String text) {
        Matcher m = DAYS_LATER.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                // asiri buyuk sayi vb.; yazi formuna dusulur
            }
        }
        for (Map.Entry<String, Integer> entry : WORD_NUMBERS.entrySet()) {
            if (text.contains(entry.getKey() + " gun sonra")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean hasSingleDayCue(String text) {
        if (text.contains("bugun") || text.contains("yarin") || text.contains("obur gun")) {
            return true;
        }
        return TurkishText.WEEKDAY_KEYWORDS.stream().anyMatch(e -> text.contains(e.getKey()));
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

        // "N gun sonra" (rakam veya yazi) — obur gun (+2) ile ayni ailenin genellemesi.
        Integer daysLater = extractDaysLater(text);
        if (daysLater != null) {
            return today.plusDays(daysLater);
        }

        boolean nextWeek = text.contains("gelecek") || text.contains("onumuzdeki")
                || text.contains("haftaya");
        for (Map.Entry<String, DayOfWeek> entry : TurkishText.WEEKDAY_KEYWORDS) {
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
                .map(m -> TurkishText.dayName(m.getDate().getDayOfWeek())
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

}
