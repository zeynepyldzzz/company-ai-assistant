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

    // --- A-27 (#176): hafta ofseti goreli gun ipuclarina da uygulanir ---

    // Olculdu (chat_message_log): "haftaya bugün yemekte ne var" 0.916 ile dogru kategoriye
    // gidiyordu, yani hata siniflandirmada degil tarih cikarimindaydi — "haftaya" bilgisi
    // sessizce kayboluyor ve BUGUN donuyordu.
    @Test
    void haftayaBugunBirHaftaSonrasiniCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "haftaya bugün yemekte ne var");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    void gecenHaftaBugunBirHaftaOncesiniCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "geçen hafta bugün ne yedim");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().minusDays(7));
    }

    // Gun adi dalinda eskiden yalnizca ileri yon vardi; "geçen hafta çarşamba" BU haftanin
    // carsambasini donduruyordu.
    @Test
    void gecenHaftaGunAdiOncekiHaftayaGider() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "geçen hafta çarşamba ne vardı");

        LocalDate captured = capturedDate();
        assertThat(captured.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(captured).isEqualTo(currentMonday().minusDays(7).plusDays(2));
    }

    // NOBETCI: hafta ofseti TASIMAYAN sorular degismedi.
    @Test
    void haftaOfsetiYoksaGoreliGunlerDegismez() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "dün ne vardı");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().minusDays(1));
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

    // #104: "N gun sonra" rakam formu -> today + N.
    @Test
    void nGunSonraRakamFormuTarihiCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "2 gün sonraki menü");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    // #104: "N gun sonra" yazi formu -> today + N.
    @Test
    void nGunSonraYaziFormuTarihiCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "üç gün sonra ne var");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().plusDays(3));
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

    // #124: "dunki yemek" desteklenmiyordu, sessizce BUGUNUN menusu donuyordu.
    @Test
    void dunKelimesiOncekiGunuCeker() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "dünkü yemek neydi");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().minusDays(1));
    }

    @Test
    void nGunOnceIfadesiGecmiseGider() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "3 gün önce ne vardı");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().minusDays(3));
    }

    @Test
    void yaziIleUcGunOnceDeCalisir() {
        when(menuService.getMenuByDate(any())).thenReturn(Optional.empty());

        resolver.resolve("yemek_menusu", "üç gün önce menüde ne vardı");

        assertThat(capturedDate()).isEqualTo(LocalDate.now().minusDays(3));
    }

    // #124: "haftaya" hafta modunda goz ardi ediliyordu ve BU haftanin menusu
    // "Bu haftanın menüsü:" basligiyla donuyordu.
    @Test
    void haftayaIfadesiGelecekHaftayiIster() {
        when(menuService.getWeeklyMenu(any(LocalDate.class))).thenReturn(List.of());

        Map<String, String> vars = resolver.resolve("yemek_menusu", "haftaya menüde ne var");

        assertThat(capturedWeekAnchor()).isEqualTo(LocalDate.now().plusWeeks(1));
        assertThat(vars.get("menu_gunu")).contains("Gelecek hafta").doesNotContain("Bu haftanın menüsü");
    }

    @Test
    void gecenHaftaIfadesiOncekiHaftayiIster() {
        when(menuService.getWeeklyMenu(any(LocalDate.class))).thenReturn(List.of());

        Map<String, String> vars = resolver.resolve("yemek_menusu", "geçen hafta neler vardı");

        assertThat(capturedWeekAnchor()).isEqualTo(LocalDate.now().minusWeeks(1));
        assertThat(vars.get("menu_gunu")).contains("Geçen hafta");
    }

    private LocalDate capturedWeekAnchor() {
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(menuService).getWeeklyMenu(captor.capture());
        return captor.getValue();
    }

    @Test
    void bosHaftaIsteginde_bulunmuyorMesaji() {
        when(menuService.getWeeklyMenu(any(LocalDate.class))).thenReturn(List.of());

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
        when(menuService.getWeeklyMenu(any(LocalDate.class))).thenReturn(List.of(day));

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
