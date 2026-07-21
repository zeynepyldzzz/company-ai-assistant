import { z } from "zod";

// GET /menus/today, GET /menus/weekly (C-1)
export const MealItemSchema = z.object({
  id: z.number(),
  name: z.string(),
  calories: z.number().nullable(),
  allergens: z.string().nullable(),
});
export type MealItem = z.infer<typeof MealItemSchema>;

export const MenuSchema = z.object({
  id: z.number(),
  date: z.string(),
  weekNumber: z.number().nullable(),
  items: z.array(MealItemSchema),
});
export type Menu = z.infer<typeof MenuSchema>;

export const WeeklyMenuSchema = z.array(MenuSchema);
export type WeeklyMenu = z.infer<typeof WeeklyMenuSchema>;