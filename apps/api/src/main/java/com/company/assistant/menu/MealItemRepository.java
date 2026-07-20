package com.company.assistant.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface MealItemRepository extends JpaRepository<MealItem, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MealItem mi WHERE mi.menu.id = :menuId")
    void deleteByMenuId(Integer menuId);
}