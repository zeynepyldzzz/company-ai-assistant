package com.company.assistant.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// #194: Bugun/Bu Hafta yanina Bu Ay sekmesi - getMonthlyMenu, verilen tarihin
// ait oldugu ayin ilk/son gunu araligini repository'ye dogru gecirmeli.
class MenuServiceTest {

    private MealMenuRepository mealMenuRepository;
    private MealItemRepository mealItemRepository;
    private MenuService service;

    @BeforeEach
    void setUp() {
        mealMenuRepository = mock(MealMenuRepository.class);
        mealItemRepository = mock(MealItemRepository.class);
        service = new MenuService(mealMenuRepository, mealItemRepository);
    }

    @Test
    void getMonthlyMenu_ayinIlkVeSonGunuArasindakiMenuleriDoner() {
        MealMenu menu = new MealMenu();
        menu.setId(1);
        menu.setDate(LocalDate.of(2026, 8, 10));
        when(mealMenuRepository.findByDateBetween(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(menu));

        List<MenuResponse> result = service.getMonthlyMenu(LocalDate.of(2026, 8, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void getMonthlyMenu_subatGibiKisaAylardaSonGunuDogruHesaplar() {
        when(mealMenuRepository.findByDateBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of());

        service.getMonthlyMenu(LocalDate.of(2026, 2, 15));

        verify(mealMenuRepository).findByDateBetween(eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28)));
    }
}
