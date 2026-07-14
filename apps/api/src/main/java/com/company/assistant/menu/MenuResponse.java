package com.company.assistant.menu;

import java.time.LocalDate;
import java.util.List;

public class MenuResponse {

    private Integer id;
    private LocalDate date;
    private Integer weekNumber;
    private List<MealItemResponse> items;

    public MenuResponse(MealMenu menu) {
        this.id = menu.getId();
        this.date = menu.getDate();
        this.weekNumber = menu.getWeekNumber();
        if (menu.getItems() != null) {
            this.items = menu.getItems().stream()
                    .map(MealItemResponse::new)
                    .toList();
        }
    }

    public Integer getId() { return id; }
    public LocalDate getDate() { return date; }
    public Integer getWeekNumber() { return weekNumber; }
    public List<MealItemResponse> getItems() { return items; }
}