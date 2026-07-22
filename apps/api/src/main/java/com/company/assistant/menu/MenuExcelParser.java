package com.company.assistant.menu;

import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * Yaşar Bilgi yemekhanesinden gelen Excel şablonunu okur.
 *
 * ŞABLONUN YAPISI (yemekhane hep böyle gönderiyor, format sabit):
 *  - Sayfa içinde art arda "hafta blokları" var.
 *  - Her blok şöyle başlar:
 *      satır N   : 5 sütunda o haftanın Pazartesi-Cuma tarihleri (Excel tarih hücresi)
 *      satır N+1 : gün adları (PAZARTESİ, SALI, ...) -> kullanmıyoruz,
 *                  çünkü tarih hücresi zaten güvenilir kaynak.
 *      satır N+2..N+6 (5 satır): Çorba, Ana Yemek, Pilav/Makarna, Tatlı/İçecek, Meyve
 *        (kategori adı hücrede YAZMAZ, sadece sıraya göre biliniyor)
 *      1-2 boş satır
 *      sonraki 4 satır: Salata, Zeytinyağlı Sebze, Yardımcı Salata, Yoğurt/Cacık
 *  - Bazı sütunlar o hafta için boş olabilir (örn. ayın son haftasının Cuma günü).
 *  - Gerçek dosyalarda ara sıra FAZLADAN / HATALI satır çıkabiliyor (örn. bir
 *    önceki "Meyve" satırının kopyası tek bir sütunda tekrar etmiş gibi). Bu
 *    satırları "stray satır" diye tespit edip atlıyoruz, warning listesine yazıyoruz.
 *
 * Bu sınıf sadece OKUR, veritabanına hiçbir şey yazmaz (bkz. MenuImportService).
 */
public class MenuExcelParser {

    private static final int MAX_DAY_COLUMNS = 5; // Pazartesi..Cuma
    private static final int MAX_BLANK_ROWS_TO_SKIP = 4;

    public ParseResult parse(InputStream excelStream) throws IOException {
        List<String> warnings = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ParsedMenuDayDto> days = new ArrayList<>();

            int lastRow = sheet.getLastRowNum();
            int i = sheet.getFirstRowNum();

            while (i <= lastRow) {
                Row row = sheet.getRow(i);
                LocalDate[] dateSlots = extractDateRow(row);

                if (dateSlots == null) {
                    i++;
                    continue;
                }

                int cursor = i + 2; // gün-adı satırını atla

                Map<Integer, List<ParsedMealItemDto>> dayItems = new LinkedHashMap<>();
                Map<Integer, String> lastFruitSeen = new HashMap<>();
                for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
                    if (dateSlots[c] != null) {
                        dayItems.put(c, new ArrayList<>());
                    }
                }

                // --- ANA BLOK: Çorba, Ana Yemek, Pilav, Tatlı, Meyve ---
                for (MealCategory category : MealCategory.MAIN_BLOCK) {
                    Row catRow = sheet.getRow(cursor);
                    String[] values = readRowValues(catRow);
                    for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
                        if (dateSlots[c] != null && isNotBlank(values[c])) {
                            dayItems.get(c).add(new ParsedMealItemDto(category, values[c].trim()));
                            if (category == MealCategory.MEYVE) {
                                lastFruitSeen.put(c, values[c].trim());
                            }
                        }
                    }
                    cursor++;
                }

                // --- Boş satırları / hatalı "stray" satırları atla ---
                int skipped = 0;
                while (cursor <= lastRow && skipped < MAX_BLANK_ROWS_TO_SKIP) {
                    Row candidate = sheet.getRow(cursor);
                    String[] values = readRowValues(candidate);

                    if (isBlankRow(values)) {
                        cursor++;
                        skipped++;
                        continue;
                    }
                    if (extractDateRow(candidate) != null) {
                        break; // bir sonraki hafta bloğu araya boşluk koymadan başlamış
                    }
                    if (looksLikeStrayDuplicateRow(values, lastFruitSeen)) {
                        warnings.add("Satır " + (cursor + 1) + ": beklenmeyen/fazladan satır "
                                + "atlandı (önceki 'Meyve' satırının tekrarı gibi görünüyor): "
                                + Arrays.toString(values));
                        cursor++;
                        skipped++;
                        continue;
                    }
                    break; // gerçek salata bloğu burada başlıyor
                }

                // --- SALATA BLOKU: Salata, Zeytinyağlı Sebze, Yardımcı Salata, Yoğurt/Cacık ---
                for (MealCategory category : MealCategory.SALAD_BLOCK) {
                    Row catRow = sheet.getRow(cursor);
                    if (catRow == null || extractDateRow(catRow) != null) {
                        warnings.add("Satır " + (i + 1) + " ile başlayan hafta bloğunda salata "
                                + "kısmı eksik kaldı ('" + category.getDisplayName()
                                + "' bulunamadan yeni tarih satırına geçildi).");
                        break;
                    }
                    String[] values = readRowValues(catRow);
                    for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
                        if (dateSlots[c] != null && isNotBlank(values[c])) {
                            dayItems.get(c).add(new ParsedMealItemDto(category, values[c].trim()));
                        }
                    }
                    cursor++;
                }

                for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
                    if (dateSlots[c] != null) {
                        List<ParsedMealItemDto> items = dayItems.get(c);
                        if (items.isEmpty()) {
                            warnings.add(dateSlots[c] + " için Excel'de hiç yemek verisi bulunamadı "
                                    + "(gün sütunu var ama tüm satırlar boş).");
                        }
                        days.add(new ParsedMenuDayDto(dateSlots[c], items));
                    }
                }

                i = cursor;
            }

            return new ParseResult(days, warnings);
        }
    }

    private LocalDate[] extractDateRow(Row row) {
        if (row == null) return null;
        LocalDate[] result = new LocalDate[MAX_DAY_COLUMNS];
        boolean foundAny = false;
        for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cell)) {
                result[c] = cell.getLocalDateTimeCellValue().toLocalDate();
                foundAny = true;
            }
        }
        return foundAny ? result : null;
    }

    private String[] readRowValues(Row row) {
        String[] values = new String[MAX_DAY_COLUMNS];
        if (row == null) return values;
        DataFormatter formatter = new DataFormatter(new Locale("tr", "TR"));
        for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                values[c] = null;
            } else {
                String v = formatter.formatCellValue(cell).trim();
                values[c] = v.isEmpty() ? null : v;
            }
        }
        return values;
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isBlankRow(String[] values) {
        for (String v : values) {
            if (isNotBlank(v)) return false;
        }
        return true;
    }

    private boolean looksLikeStrayDuplicateRow(String[] values, Map<Integer, String> lastFruitSeen) {
        boolean anyNonBlank = false;
        for (int c = 0; c < MAX_DAY_COLUMNS; c++) {
            if (isNotBlank(values[c])) {
                anyNonBlank = true;
                if (!values[c].equals(lastFruitSeen.get(c))) {
                    return false;
                }
            }
        }
        return anyNonBlank;
    }

    public record ParseResult(List<ParsedMenuDayDto> days, List<String> warnings) {
    }
}