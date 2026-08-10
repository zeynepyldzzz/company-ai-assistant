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

import com.company.assistant.common.DateExpression;
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

    /**
     * A-37 (#203): tarih ifadesi var ama cozulemedi (or. yalniz "agustos", "32 agustos").
     * Kullaniciya calisan bir alternatif gosteriliyor — A-26'daki olumsuz sorgu mesajiyla
     * ayni yaklasim: neyi yapamadigimizi soyle, ne yapabilecegini goster.
     */
    private static final String UNRESOLVED_DATE =
            "Hangi tarihi sorduğunu tam anlayamadım. \"bugün\", \"yarın\", \"çarşamba\" "
                    + "ya da \"17 Ağustos\" gibi yazabilirsin.";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    // "N gun sonra" / "N gun once" — rakam formu. Yazi formu ayri haritada.
    private static final Pattern DAYS_LATER = Pattern.compile("(\\d+)\\s*gun\\s+sonra");
    private static final Pattern DAYS_AGO = Pattern.compile("(\\d+)\\s*gun\\s+once");
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
            // #124: "haftaya"/"gelecek hafta" ifadeleri eskiden burada goz ardi ediliyor ve
            // sessizce BU haftanin menusu "Bu haftanın menüsü:" basligiyla donuyordu.
            int weekOffset = weekOffset(text);
            List<MenuResponse> week = menuService.getWeeklyMenu(LocalDate.now().plusWeeks(weekOffset));
            if (week.isEmpty()) {
                // Menu yokken "... menusu:" basligi yaniltici olur; net cumle.
                variables.put("menu_gunu", switch (weekOffset) {
                    case 1 -> "Gelecek hafta için menü girilmemiş görünüyor.";
                    case -1 -> "Geçen hafta için menü kaydı bulunmuyor.";
                    default -> "Bu hafta için menü bulunmuyor.";
                });
                variables.put("gunun_menusu", "");
            } else {
                variables.put("menu_gunu", switch (weekOffset) {
                    case 1 -> "Gelecek haftanın menüsü:";
                    case -1 -> "Geçen haftanın menüsü:";
                    default -> "Bu haftanın menüsü:";
                });
                variables.put("gunun_menusu", buildWeekBody(week));
            }
            return variables;
        }

        // A-37 (#203): ACIK tarih ifadesi ("17 agustos", "17.08") varsa once o denenir.
        // Cozulemezse BUGUNE DUSULMEZ — asagidaki resolveTarget() taninmayan her ifadeyi
        // bugune cevirdigi icin kullanici 17 Agustos'u sorup bugunun menusunu aliyordu ve
        // baslikta tarih yazsa bile fark etmeyebiliyordu. Sessizce yanlis gun gostermektense
        // anlamadigimizi soylemek dogru (A-26'da olumsuz sorgular icin verdigimiz kararla ayni).
        if (DateExpression.mentionsDate(text)) {
            Optional<LocalDate> explicit = DateExpression.resolve(text, LocalDate.now());
            if (explicit.isEmpty()) {
                variables.put("menu_gunu", UNRESOLVED_DATE);
                variables.put("gunun_menusu", "");
                return variables;
            }
            return dayMenu(explicit.get(), variables);
        }

        return dayMenu(resolveTarget(text, LocalDate.now()), variables);
    }

    private Map<String, String> dayMenu(LocalDate target, Map<String, String> variables) {
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
        return extractDayCount(text, DAYS_LATER, " gun sonra");
    }

    // #124: "3 gun once" / "uc gun once" -> 3. Ileri yonun aynadaki karsiligi.
    private Integer extractDaysAgo(String text) {
        return extractDayCount(text, DAYS_AGO, " gun once");
    }

    private Integer extractDayCount(String text, Pattern digitPattern, String wordSuffix) {
        Matcher m = digitPattern.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                // asiri buyuk sayi vb.; yazi formuna dusulur
            }
        }
        for (Map.Entry<String, Integer> entry : WORD_NUMBERS.entrySet()) {
            if (text.contains(entry.getKey() + wordSuffix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isNextWeek(String text) {
        return text.contains("gelecek") || text.contains("onumuzdeki") || text.contains("haftaya");
    }

    private boolean isLastWeek(String text) {
        return text.contains("gecen hafta") || text.contains("onceki hafta");
    }

    /** Hafta modunda hedef hafta: -1 gecen, 0 bu, +1 gelecek. */
    private int weekOffset(String text) {
        if (isLastWeek(text)) {
            return -1;
        }
        return isNextWeek(text) ? 1 : 0;
    }

    private boolean hasSingleDayCue(String text) {
        if (text.contains("bugun") || text.contains("yarin") || text.contains("obur gun")
                || text.contains("dun") || text.contains("onceki gun")) {
            return true;
        }
        return TurkishText.WEEKDAY_KEYWORDS.stream().anyMatch(e -> text.contains(e.getKey()));
    }

    /**
     * Ham (ASCII'ye katlanmis) mesajdan hedef tarihi cikarir.
     *
     * <p>A-27 (#176): hafta ofseti GORELI gun ipuclarina da uygulanir. Onceden yalnizca hafta
     * gunu adlarinda uygulaniyordu, dolayisiyla "gelecek hafta çarşamba" dogru calisirken
     * "haftaya bugün" cumlesindeki hafta bilgisi sessizce kayboluyor ve BUGUN donuyordu —
     * ayni dosyada iki farkli davranis. Olculdu: "haftaya bugün yemekte ne var" 0.916 ile
     * dogru kategoriye gidiyordu, yani hata siniflandirmada degil buradaydi.
     */
    private LocalDate resolveTarget(String text, LocalDate today) {
        long weekShift = weekOffset(text) * 7L;

        if (text.contains("bugun")) {
            return today.plusDays(weekShift);
        }
        if (text.contains("yarin")) {
            return today.plusDays(1 + weekShift);
        }
        if (text.contains("obur gun")) {
            return today.plusDays(2 + weekShift);
        }
        // #124: gecmis yon. "dunki yemek" desteklenmiyordu ve bugune dusuyordu.
        if (text.contains("dun")) {
            return today.plusDays(-1 + weekShift);
        }
        if (text.contains("onceki gun") || text.contains("evvelki gun")) {
            return today.plusDays(-2 + weekShift);
        }

        // "N gun sonra" (rakam veya yazi) — obur gun (+2) ile ayni ailenin genellemesi.
        Integer daysLater = extractDaysLater(text);
        if (daysLater != null) {
            return today.plusDays(daysLater);
        }
        Integer daysAgo = extractDaysAgo(text);
        if (daysAgo != null) {
            return today.minusDays(daysAgo);
        }

        // A-27: burada eskiden yalnizca nextWeek vardi, gecmis yon desteklenmiyordu —
        // "geçen hafta çarşamba" BU haftanin carsambasini donduruyordu.
        for (Map.Entry<String, DayOfWeek> entry : TurkishText.WEEKDAY_KEYWORDS) {
            if (text.contains(entry.getKey())) {
                LocalDate day = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusDays(entry.getValue().getValue() - 1L);
                return day.plusDays(weekShift);
            }
        }

        // Gun belirtilmemis/taninmamis -> bugun (hafta ofseti varsa ona kaydirilmis hali).
        return today.plusDays(weekShift);
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
