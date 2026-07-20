ALTER TABLE meal_item
    ADD COLUMN category VARCHAR(30),
    ADD COLUMN sort_order INTEGER;

CREATE INDEX idx_meal_item_menu_sort ON meal_item (menu_id, sort_order);