package com.company.assistant.menu;

public class MealItemResponse {

    private Integer id;
    private String name;
    private Integer calories;
    private String allergens;

    public MealItemResponse(MealItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.calories = item.getCalories();
        this.allergens = item.getAllergens();
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Integer getCalories() { return calories; }
    public String getAllergens() { return allergens; }
}