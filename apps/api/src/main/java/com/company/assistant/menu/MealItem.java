package com.company.assistant.menu;

import jakarta.persistence.*;

@Entity
@Table(name = "meal_item")
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MealMenu menu;

    private String name;

    private Integer calories;

    private String allergens;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public MealMenu getMenu() { return menu; }
    public void setMenu(MealMenu menu) { this.menu = menu; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public String getAllergens() { return allergens; }
    public void setAllergens(String allergens) { this.allergens = allergens; }
}
