package com.company.assistant.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST/PUT /admin/employees govdesi.
 *
 * <p>A-29 (#178): {@code password} alani KALDIRILDI. Admin artik hicbir yolla sifre
 * belirlemiyor — olusturmada sistem gecici bir sifre uretiyor, sifirlama icin ayri bir uc
 * var (POST /admin/employees/{id}/reset-password).
 *
 * <p>Gerekce: admin'in bir calisanin sifresini bilmesi, sifreyi kimlik dogrulama araci
 * olmaktan cikarir. Iki yol (admin girer / sistem uretir) birakildiginda ikisinin de sonucu
 * gecici sifre oluyordu; ikinci yolu tutup birincisini kaldirmak hem kodu hem arayuzu
 * sadelestiriyor. Kural tek cumleye indi: sifreyi yalnizca kullanicinin kendisi belirler.
 */
public record AdminEmployeeRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String officeStatus,
        Integer departmentId,
        Integer roleId) {
}
