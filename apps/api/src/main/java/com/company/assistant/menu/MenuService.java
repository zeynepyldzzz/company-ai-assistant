package com.company.assistant.menu;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MenuService {

    private final MealMenuRepository mealMenuRepository;
    private final MealItemRepository mealItemRepository;

    public MenuService(MealMenuRepository mealMenuRepository, MealItemRepository mealItemRepository) {
        this.mealMenuRepository = mealMenuRepository;
        this.mealItemRepository = mealItemRepository;
    }

    public MenuResponse getTodayMenu() {
        LocalDate today = LocalDate.now();
        MealMenu menu = mealMenuRepository.findByDate(today)
                .orElseThrow(() -> new MenuNotFoundException("Bugun icin tanimli menu bulunamadi: " + today));
        return new MenuResponse(menu);
    }

    /**
     * Belirli bir tarihin menusu. getTodayMenu()'den farki: menu yoksa exception
     * firlatmaz, bos Optional doner — cagiran ( or. chatbot menu resolver) "o gun icin
     * menu girilmemis" gibi nazik bir yanit uretebilsin, exception akis kontrolu olmasin.
     * A-11: bu metot ayni zamanda ileride (Faz 2) LLM'in cagiracagi "arac"tir.
     */
    public Optional<MenuResponse> getMenuByDate(LocalDate date) {
        return mealMenuRepository.findByDate(date).map(MenuResponse::new);
    }

    public List<MenuResponse> getWeeklyMenu() {
        return getWeeklyMenu(LocalDate.now());
    }

    /**
     * Verilen tarihin ait oldugu haftanin menusu. A-17 (#124): "haftaya menude ne var"
     * sorusu once sessizce BU haftanin menusunu donduruyordu; chatbot resolver'i artik
     * gelecek haftanin bir gununu vererek dogru haftayi ister.
     */
    public List<MenuResponse> getWeeklyMenu(LocalDate anyDayOfWeek) {
        int week = anyDayOfWeek.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        List<MealMenu> menus = mealMenuRepository.findByWeekNumber(week);
        return menus.stream().map(MenuResponse::new).toList();
    }

    public MealItemResponse getMealById(Integer id) {
        MealItem item = mealItemRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Yemek bulunamadi, id: " + id));
        return new MealItemResponse(item);
    }
}