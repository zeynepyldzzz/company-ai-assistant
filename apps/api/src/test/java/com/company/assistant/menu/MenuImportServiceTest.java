package com.company.assistant.menu;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}