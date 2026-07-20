package com.company.assistant.menu;

/**
 * Yemekhane Excel'inde satır SIRASINA göre kategori belirlenir (hücrede kategori
 * adı yazmaz). Bu yüzden enum'daki sıralama = Excel'deki satır sıralamasıyla
 * birebir aynı olmak ZORUNDA. Sırayı değiştirirsen parser da bozulur.
 *
 * Ana blok (gün adı satırından hemen sonraki 5 satır):
 *   ÇORBA, ANA_YEMEK, PILAV_MAKARNA, TATLI_ICECEK, MEYVE
 * Salata bloğu (1-2 boş satırdan sonraki 4 satır):
 *   SALATA, ZEYTINYAGLI_SEBZE, YARDIMCI_SALATA, YOGURT_CACIK
 */
public enum MealCategory {
    CORBA(1, "Çorba"),
    ANA_YEMEK(2, "Ana Yemek"),
    PILAV_MAKARNA(3, "Pilav / Makarna"),
    TATLI_ICECEK(4, "Tatlı / İçecek"),
    MEYVE(5, "Meyve"),
    SALATA(6, "Salata"),
    ZEYTINYAGLI_SEBZE(7, "Zeytinyağlı Sebze"),
    YARDIMCI_SALATA(8, "Yardımcı Salata / Turşu"),
    YOGURT_CACIK(9, "Yoğurt / Cacık");

    /** Ana blok kategorileri, Excel'deki satır sırasıyla aynı sırada. */
    public static final MealCategory[] MAIN_BLOCK = {
            CORBA, ANA_YEMEK, PILAV_MAKARNA, TATLI_ICECEK, MEYVE
    };

    /** Salata bloğu kategorileri, Excel'deki satır sırasıyla aynı sırada. */
    public static final MealCategory[] SALAD_BLOCK = {
            SALATA, ZEYTINYAGLI_SEBZE, YARDIMCI_SALATA, YOGURT_CACIK
    };

    private final int sortOrder;
    private final String displayName;

    MealCategory(int sortOrder, String displayName) {
        this.sortOrder = sortOrder;
        this.displayName = displayName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDisplayName() {
        return displayName;
    }
}