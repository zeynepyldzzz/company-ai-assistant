package com.company.assistant.menu;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C-1 issue'sunda istenen 3 endpoint burada tanımlıdır:
 *   GET /menus/today
 *   GET /menus/weekly
 *   GET /meals/{id}
 */
@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus/today")
    public MenuResponse getTodayMenu() {
        return menuService.getTodayMenu();
    }

    @GetMapping("/menus/weekly")
    public List<MenuResponse> getWeeklyMenu() {
        return menuService.getWeeklyMenu();
    }

    @GetMapping("/meals/{id}")
    public MealItemResponse getMealById(@PathVariable Integer id) {
        return menuService.getMealById(id);
    }

    // Menü veya yemek bulunamazsa, kullanıcıya anlamlı bir 404 hatası döndürür.
    @ExceptionHandler(MenuNotFoundException.class)
    public ResponseEntity<String> handleNotFound(MenuNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}