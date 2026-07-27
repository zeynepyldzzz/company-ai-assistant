package com.company.assistant.report;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// C-T4 (#86): usage raporu export'unun (xlsx) gercekten gecerli, POI ile
// tekrar acilabilir bir dosya urettigini dogrular (C-11 kabul kriteri).
class ReportExportServiceTest {

    private final ReportExportService service = new ReportExportService();

    @Test
    void xlsxCiktisi_gecerliVeIcerigiDogru() throws Exception {
        UsageReportResponse report = new UsageReportResponse(
                "usage",
                Instant.parse("2026-07-27T00:00:00Z"),
                List.of(
                        new UsageReportRow("Chatbot", "Toplam Soru", 42),
                        new UsageReportRow("Anket", "Toplam Yanit", 7)));

        byte[] bytes = service.toXlsx(report);

        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("usage");
            assertThat(sheet).isNotNull();

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Modul");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Metrik");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Sayi");

            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Chatbot");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Toplam Soru");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(42);

            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Anket");
            assertThat(sheet.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(7);
        }
    }

    @Test
    void bosSatirListesiyle_yalnizcaBasligiOlanGecerliDosyaUretir() throws Exception {
        UsageReportResponse report = new UsageReportResponse(
                "usage", Instant.parse("2026-07-27T00:00:00Z"), List.of());

        byte[] bytes = service.toXlsx(report);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheet("usage");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
        }
    }
}
