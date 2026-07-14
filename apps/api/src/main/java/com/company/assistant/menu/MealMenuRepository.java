package com.company.assistant.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealMenuRepository extends JpaRepository<MealMenu, Integer> {

    Optional<MealMenu> findByDate(LocalDate date);

    List<MealMenu> findByWeekNumber(Integer weekNumber);
}