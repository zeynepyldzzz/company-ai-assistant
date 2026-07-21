package com.company.assistant.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class MenuImportServiceTest {

    private MealMenuRepository mealMenuRepository;
    private MealItemRepository mealItemRepository;
    private MenuImportService service;

    @BeforeEach
    void setUp() {
        mealMenuRepository = mock(MealMenuRepository.class);
        mealItemRepository = mock(MealItemRepository.class);
        service = new MenuImportService(mealMenuRepository, mealItemRepository);
    }

    // ---------- HATA YOLLARI (kabul kriteri 2) ----------

    @Test
    void bosDosyaGonderilirseAnlamliHataFirlatir() {
        MockMultipartFile bosDosya = new MockMultipartFile(
                "file", "menu.xlsx",
                "application/octet-stream", new byte[0]);

        assertThatThrownBy(() -> service.importExcel(bosDosya, false))
                .isInstanceOf(MenuImportException.class)
                .hasMessageContaining("boş veya eksik");
    }

    @Test
    void yanlisUzantiliDosyaReddedilir() {
        MockMultipartFile pdfDosya = new MockMultipartFile(
                "file", "menu.pdf",
                "application/pdf", "sahte icerik".getBytes());

        assertThatThrownBy(() -> service.importExcel(pdfDosya, false))
                .isInstanceOf(MenuImportException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    void gecersizIcerikliDosyadaHataFirlatir() {
        MockMultipartFile bozukDosya = new MockMultipartFile(
                "file", "menu.xlsx",
                "application/octet-stream", "bu bir excel degil".getBytes());

        assertThatThrownBy(() -> service.importExcel(bozukDosya, false))
                .isInstanceOf(MenuImportException.class);
    }

    // ---------- MUTLU YOL (kabul kriteri 1) ----------

    @Test
    void gecerliSablondaKayitlarDogruOlusturulur() throws Exception {
        MockMultipartFile dosya = new MockMultipartFile(
                "file", "menu.xlsx",
                "application/octet-stream", ikiGunlukGecerliExcel());

        when(mealMenuRepository.findByDate(any())).thenReturn(Optional.empty());
        when(mealMenuRepository.save(any(MealMenu.class)))
                .thenAnswer(cagri -> cagri.getArgument(0));

        MenuImportResponse yanit = service.importExcel(dosya, true);

        // Yanit dogru mu?
        assertThat(yanit.committed()).isTrue();
        assertThat(yanit.daysFound()).isEqualTo(2);
        assertThat(yanit.daysWithNoData()).isZero();

        // Iki gun icin MENU kaydi olustu mu?
        ArgumentCaptor<MealMenu> menuYakala = ArgumentCaptor.forClass(MealMenu.class);
        verify(mealMenuRepository, times(2)).save(menuYakala.capture());
        assertThat(menuYakala.getAllValues())
                .extracting(MealMenu::getDate)
                .containsExactly(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 21));

        // MENU_ITEM kayitlari dogru isimlerle olustu mu? (gun basina 9 kategori x 2 gun = 18)
        ArgumentCaptor<MealItem> itemYakala = ArgumentCaptor.forClass(MealItem.class);
        verify(mealItemRepository, times(18)).save(itemYakala.capture());
        assertThat(itemYakala.getAllValues())
                .extracting(MealItem::getName)
                .contains("MERCIMEK CORBA", "EZOGELIN CORBA", "IZMIR KOFTE", "KURU FASULYE");
    }

    @Test
    void onizlemeModundaVeritabaninaHicYazilmaz() throws Exception {
        MockMultipartFile dosya = new MockMultipartFile(
                "file", "menu.xlsx",
                "application/octet-stream", ikiGunlukGecerliExcel());

        MenuImportResponse yanit = service.importExcel(dosya, false);

        assertThat(yanit.committed()).isFalse();
        assertThat(yanit.daysFound()).isEqualTo(2);
        verify(mealMenuRepository, never()).save(any());
        verify(mealItemRepository, never()).save(any());
    }

    // ---------- Yardimci: parser'in bekledigi sablonda mini Excel uretir ----------

    /**
     * Sablon (MenuExcelParser'daki yapinin aynisi):
     *  satir 0: tarihler (2 gun: 20-21 Temmuz 2026)
     *  satir 1: gun adlari (parser kullanmiyor ama sablonda var)
     *  satir 2-6: ana blok  (Corba, Ana Yemek, Pilav, Tatli, Meyve)
     *  satir 7: bos
     *  satir 8-11: salata bloku (Salata, Zeytinyagli, Yardimci Salata, Yogurt)
     */
    private byte[] ikiGunlukGecerliExcel() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Menu");
            CreationHelper helper = wb.getCreationHelper();
            CellStyle tarihStili = wb.createCellStyle();
            tarihStili.setDataFormat(helper.createDataFormat().getFormat("dd.mm.yyyy"));

            Row tarihSatiri = sheet.createRow(0);
            tarihHucresi(tarihSatiri, 0, LocalDate.of(2026, 7, 20), tarihStili);
            tarihHucresi(tarihSatiri, 1, LocalDate.of(2026, 7, 21), tarihStili);

            metinSatiri(sheet, 1, "PAZARTESI", "SALI");

            metinSatiri(sheet, 2, "MERCIMEK CORBA", "EZOGELIN CORBA"); // Corba
            metinSatiri(sheet, 3, "IZMIR KOFTE", "KURU FASULYE");       // Ana yemek
            metinSatiri(sheet, 4, "PIRINC PILAVI", "BULGUR PILAVI");    // Pilav/Makarna
            metinSatiri(sheet, 5, "SUTLAC", "REVANI");                  // Tatli/Icecek
            metinSatiri(sheet, 6, "MEYVE", "MEYVE");                    // Meyve
            // satir 7 bos birakiliyor
            metinSatiri(sheet, 8, "COBAN SALATA", "MEVSIM SALATA");     // Salata
            metinSatiri(sheet, 9, "ZY. BROKOLI", "ZY. ENGINAR");        // Zeytinyagli
            metinSatiri(sheet, 10, "TURSU", "KORNISON");                // Yardimci salata
            metinSatiri(sheet, 11, "YOGURT", "CACIK");                  // Yogurt/Cacik

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void tarihHucresi(Row satir, int sutun, LocalDate tarih, CellStyle stil) {
        Cell hucre = satir.createCell(sutun);
        hucre.setCellValue(tarih);
        hucre.setCellStyle(stil);
    }

    private void metinSatiri(Sheet sheet, int satirNo, String... degerler) {
        Row satir = sheet.createRow(satirNo);
        for (int c = 0; c < degerler.length; c++) {
            satir.createCell(c).setCellValue(degerler[c]);
        }
    }
}