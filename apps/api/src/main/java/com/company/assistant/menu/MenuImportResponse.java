package com.company.assistant.menu;

import java.util.List;

/**
 * /admin/menus/import endpoint'inin cevabı.
 *
 * committed = false -> sadece ÖNİZLEME, veritabanına hiçbir şey yazılmadı.
 * committed = true  -> veriler veritabanına yazıldı (upsert edildi).
 *
 * warnings -> parser'ın "beklediğim gibi gitmedi ama devam ettim" dediği
 *             noktalar (örn. fazladan/hatalı satır atlandı).
 */
public record MenuImportResponse(
        boolean committed,
        int daysFound,
        int daysWithNoData,
        List<ParsedMenuDayDto> days,
        List<String> warnings
) {
}