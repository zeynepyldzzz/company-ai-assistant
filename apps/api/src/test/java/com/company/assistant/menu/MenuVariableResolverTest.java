package com.company.assistant.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuVariableResolverTest {

    @Mock
    private MenuService menuService;

    private MenuVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MenuVariableResolver(menuService);
    }

    @Test
    void menuDisiIntentBosDoner() {
        assertThat(resolver.resolve("izin_prosedur", "yıllık izin")).isEmpty();
        verify(menuService, never()).getMenuByDate(any());
    }

    @Test
    void bugunKelimesiBugununTarihiniCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "bugün ne var");

        assertThat(capturedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void yarinKelimesiErtesiGunuCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "yarın yemekte ne var");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void gunAdiBuHaftakiOGuneCozulur() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "çarşamba menüsü");

        LocalDate captured = capturedDate();
        assertThat(captured.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(captured).isBetween(currentMonday(), currentMonday().plusDays(6));
    }

    @Test
    void turkceKaraktersizGunAdiDaEslesir() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "carsamba ne var");

        assertThat(capturedDate().getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    void gelecekHaftaOnekiGunuBirHaftaIleriTasir() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "gelecek hafta cuma ne var");

        LocalDate captured = capturedDate();
        assertThat(captured.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(captured).isAfterOrEqualTo(currentMonday().plusDays(7));
    }

    // #104 regresyon (#1): "haftaya <gun>" de bir hafta ileri tasimali (onceden bu haftayi veriyordu).
    @Test
    void haftayaGunuBirHaftaIleriTasir() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "haftaya çarşamba ne var");

        LocalDate captured = capturedDate();
        assertThat(captured.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(captured).isAfterOrEqualTo(currentMonday().plusDays(7));
    }

    // #104 regresyon (#2): menu yoksa "... menusu:" basligi basilmamali; net "bulunmuyor" cumlesi.
    @Test
    void menuYoksaYanilticiBaslikBasilmaz() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        Map<String, String> vars = resolver.resolve("yemek_menusu", "cumartesi ne var");

        assertThat(vars.get("menu_gunu")).contains("için menü bulunmuyor").doesNotContain("menüsü:");
        assertThat(vars.get("gunun_menusu")).isEmpty();
    }

    @Test
    void bosHaftaIsteginde_bulunmuyorMesaji() {
        when(menuService.getWeeklyMenu()).thenReturn(List.of());

        Map<String, String> vars = resolver.resolve("yemek_menusu", "bu haftanın listesi");

        assertThat(vars.get("menu_gunu")).isEqualTo("Bu hafta için menü bulunmuyor.");
        verify(menuService, never()).getMenuByDate(any());
    }

    @Test
    void doluHaftaIstegiTumHaftayiListeler() {
        MenuResponse day = mock(MenuResponse.class);
        when(day.getDate()).thenReturn(LocalDate.of(2026, 7, 29));
        MealItemResponse item = mock(MealItemResponse.class);
        when(item.getName()).thenReturn("Çorba");
        when(day.getItems()).thenReturn(List.of(item));
        when(menuService.getWeeklyMenu()).thenReturn(List.of(day));

        Map<String, String> vars = resolver.resolve("yemek_menusu", "bu haftanın listesi");

        assertThat(vars.get("menu_gunu")).isEqualTo("Bu haftanın menüsü:");
        verify(menuService, never()).getMenuByDate(any());
    }

    @Test
    void menuVarsaKalemleriMaddeliListelenir() {
        MealItemResponse item = mock(MealItemResponse.class);
        when(item.getName()).thenReturn("Mercimek çorbası");
        when(item.getCalories()).thenReturn(250);
        MenuResponse menu = mock(MenuResponse.class);
        when(menu.getItems()).thenReturn(List.of(item));
        when(menuService.getMenuByDate(any())).thenReturn(Optional.of(menu));

        Map<String, String> vars = resolver.resolve("yemek_menusu", "bugün ne var");

        assertThat(vars.get("menu_gunu")).contains("menüsü:");
        assertThat(vars.get("gunun_menusu")).isEqualTo("• Mercimek çorbası (250 kcal)");
    }

    private LocalDate capturedDate() {
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(menuService).getMenuByDate(captor.capture());
        return captor.getValue();
    }

    private LocalDate currentMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
