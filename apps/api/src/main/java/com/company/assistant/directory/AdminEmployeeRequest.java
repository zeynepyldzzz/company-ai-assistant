package com.company.assistant.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST/PUT /admin/employees govdesi.
 * roleId ve departmentId opsiyonel (department atanmamis / rol default employee olabilir).
 *
 * C-12 (#120): password alani eklendi. Olusturma (POST) sirasinda zorunlu
 * (AdminEmployeeService bunu kontrol eder, @NotBlank koymuyoruz cunku ayni
 * record guncelleme (PUT) icin de kullaniliyor ve guncellemede sifre
 * degistirmek istemiyorsa bos birakilabilir).
 */
public record AdminEmployeeRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String officeStatus,
        Integer departmentId,
        Integer roleId,
        String password) {
}
