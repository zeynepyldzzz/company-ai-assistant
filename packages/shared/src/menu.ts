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
  weekNumber: z.number().nullish(),
  items: z.array(MealItemSchema),
});
export type Menu = z.infer<typeof MenuSchema>;

export const WeeklyMenuSchema = z.array(MenuSchema);
export type WeeklyMenu = z.infer<typeof WeeklyMenuSchema>;

// POST /admin/menus/import yaniti (C-2).
export const ParsedMealItemSchema = z.object({
  category: z.string(),
  name: z.string(),
});
export type ParsedMealItem = z.infer<typeof ParsedMealItemSchema>;

export const ParsedMenuDaySchema = z.object({
  date: z.string(),
  items: z.array(ParsedMealItemSchema),
});
export type ParsedMenuDay = z.infer<typeof ParsedMenuDaySchema>;

export const MenuImportResponseSchema = z.object({
  committed: z.boolean(),
  daysFound: z.number(),
  daysWithNoData: z.number(),
  days: z.array(ParsedMenuDaySchema),
  warnings: z.array(z.string()),
});
export type MenuImportResponse = z.infer<typeof MenuImportResponseSchema>;