package com.company.assistant.menu;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

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

    public List<MenuResponse> getWeeklyMenu() {
        int currentWeek = LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        List<MealMenu> menus = mealMenuRepository.findByWeekNumber(currentWeek);
        return menus.stream().map(MenuResponse::new).toList();
    }

    public MealItemResponse getMealById(Integer id) {
        MealItem item = mealItemRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Yemek bulunamadi, id: " + id));
        return new MealItemResponse(item);
    }
}