-- Menu ekranı bos veriyle 404 donuyordu (meal_menu/meal_item hic seed edilmemisti).
-- Yasar Bilgi Temmuz 2026 menusunun 27 Temmuz haftasi (Pazartesi-Persembe, Cuma bu
-- haftada yok) gercek verileriyle eklendi. Excel'de kalori/alerjen bilgisi olmadigi
-- icin bu alanlar NULL birakildi (C-2 gorusmesiyle ilgili, bkz proje notlari).

INSERT INTO meal_menu (date, week_number) VALUES
    ('2026-07-27', 31),
    ('2026-07-28', 31),
    ('2026-07-29', 31),
    ('2026-07-30', 31);

-- 27 Temmuz Pazartesi
INSERT INTO meal_item (menu_id, name, calories, allergens, category, sort_order)
SELECT id, 'EZOGELİN ÇORBA', NULL::integer, NULL::varchar, 'CORBA', 1 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'İSLİM KÖFTE', NULL::integer, NULL::varchar, 'ANA_YEMEK', 2 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'PEYNİRLİ MAKARNA', NULL::integer, NULL::varchar, 'PILAV_MAKARNA', 3 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'KEKLİ SUPANGLE', NULL::integer, NULL::varchar, 'TATLI_ICECEK', 4 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'MEYVE', NULL::integer, NULL::varchar, 'MEYVE', 5 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'KARIŞIK SALATA', NULL::integer, NULL::varchar, 'SALATA', 6 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'YOĞURTLU İTALYAN SALATA', NULL::integer, NULL::varchar, 'ZEYTINYAGLI_SEBZE', 7 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'BİBERİYELİ KORNİŞON', NULL::integer, NULL::varchar, 'YARDIMCI_SALATA', 8 FROM meal_menu WHERE date = '2026-07-27'
UNION ALL
SELECT id, 'YOĞURT', NULL::integer, NULL::varchar, 'YOGURT_CACIK', 9 FROM meal_menu WHERE date = '2026-07-27';

-- 28 Temmuz Salı
INSERT INTO meal_item (menu_id, name, calories, allergens, category, sort_order)
SELECT id, 'TAVUK SUYU ÇORBA', NULL::integer, NULL::varchar, 'CORBA', 1 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'GEMİCİ USULÜ KURU FASULYE', NULL::integer, NULL::varchar, 'ANA_YEMEK', 2 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'ARPA ŞEH. PİRİNÇ PİLAVI', NULL::integer, NULL::varchar, 'PILAV_MAKARNA', 3 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'KADİFE TATLISI', NULL::integer, NULL::varchar, 'TATLI_ICECEK', 4 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'MEYVE', NULL::integer, NULL::varchar, 'MEYVE', 5 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'ÇOBAN SALATA', NULL::integer, NULL::varchar, 'SALATA', 6 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'KARIŞIK TURŞU', NULL::integer, NULL::varchar, 'ZEYTINYAGLI_SEBZE', 7 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'YOĞURTLU PATATES SALATASI', NULL::integer, NULL::varchar, 'YARDIMCI_SALATA', 8 FROM meal_menu WHERE date = '2026-07-28'
UNION ALL
SELECT id, 'CACIK', NULL::integer, NULL::varchar, 'YOGURT_CACIK', 9 FROM meal_menu WHERE date = '2026-07-28';

-- 29 Temmuz Çarşamba
INSERT INTO meal_item (menu_id, name, calories, allergens, category, sort_order)
SELECT id, 'MERCİMEK ÇORBA', NULL::integer, NULL::varchar, 'CORBA', 1 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'TAVUK ŞİŞ / PATATES', NULL::integer, NULL::varchar, 'ANA_YEMEK', 2 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'BULGUR PİLAVI', NULL::integer, NULL::varchar, 'PILAV_MAKARNA', 3 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'PINAR İÇECEK', NULL::integer, NULL::varchar, 'TATLI_ICECEK', 4 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'MEYVE', NULL::integer, NULL::varchar, 'MEYVE', 5 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'ÇİNGENE SALATA', NULL::integer, NULL::varchar, 'SALATA', 6 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'FIRIN MÜCVER', NULL::integer, NULL::varchar, 'ZEYTINYAGLI_SEBZE', 7 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'ATOM', NULL::integer, NULL::varchar, 'YARDIMCI_SALATA', 8 FROM meal_menu WHERE date = '2026-07-29'
UNION ALL
SELECT id, 'YOĞURT', NULL::integer, NULL::varchar, 'YOGURT_CACIK', 9 FROM meal_menu WHERE date = '2026-07-29';

-- 30 Temmuz Perşembe
INSERT INTO meal_item (menu_id, name, calories, allergens, category, sort_order)
SELECT id, 'TANDIR ÇORBA', NULL::integer, NULL::varchar, 'CORBA', 1 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'YAZ TÜRLÜSÜ', NULL::integer, NULL::varchar, 'ANA_YEMEK', 2 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'NOHUTLU PİLAV', NULL::integer, NULL::varchar, 'PILAV_MAKARNA', 3 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'KAZANDİBİ', NULL::integer, NULL::varchar, 'TATLI_ICECEK', 4 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'MEYVE', NULL::integer, NULL::varchar, 'MEYVE', 5 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'MISIRLI GÖBEK SALATA', NULL::integer, NULL::varchar, 'SALATA', 6 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'HAVUÇ KALYE', NULL::integer, NULL::varchar, 'ZEYTINYAGLI_SEBZE', 7 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'KIRMIZI KÖZ BİBER', NULL::integer, NULL::varchar, 'YARDIMCI_SALATA', 8 FROM meal_menu WHERE date = '2026-07-30'
UNION ALL
SELECT id, 'YOĞURT', NULL::integer, NULL::varchar, 'YOGURT_CACIK', 9 FROM meal_menu WHERE date = '2026-07-30';
