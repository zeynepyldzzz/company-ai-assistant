package com.company.assistant.directory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST/PUT /admin/departments govdesi. managerId opsiyonel.
 *
 * <p>A-44 (#219): uzunluk sinirlari eklendi. SINIRIN KAYNAGI MIGRATION'daki kolon genisligi
 * ({@code V1__init.sql}: {@code department.name VARCHAR(150)}). Onceden hicbir sinir yoktu ve
 * kolonu asan girdi temiz bir 400 degil {@code DataIntegrityViolationException} -> 500
 * uretiyordu.
 *
 * <p>{@code responsibilities} kolonu TEXT, yani DB sinirlamiyor; 2000 URUN karari — sinirsiz
 * girdi hem arayuzu bozuyor hem kotuye kullanima aciktu.
 *
 * <p>Mesajlar Turkce ve kullaniciya GORUNUR: {@code GlobalExceptionHandler.handleValidation}
 * alanin mesajini dogrudan yanit govdesine koyuyor.
 */
public record AdminDepartmentRequest(
        @NotBlank
        @Size(max = 150, message = "Departman adı 150 karakteri aşamaz")
        String name,

        @Size(max = 2000, message = "Sorumluluklar 2000 karakteri aşamaz")
        String responsibilities,

        Integer managerId) {
}
