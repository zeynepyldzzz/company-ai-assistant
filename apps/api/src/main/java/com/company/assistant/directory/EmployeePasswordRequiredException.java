package com.company.assistant.directory;

/**
 * C-12 (#120): yeni calisan olustururken sifre alani bos birakilirsa firlatilir.
 * Guncellemede (PUT) sifre opsiyoneldir (bos ise mevcut sifre korunur), ama
 * olusturmada (POST) bir ilk sifre atanmasi zorunludur; aksi halde kullanici
 * hicbir zaman giris yapamaz.
 */
public class EmployeePasswordRequiredException extends RuntimeException {

    public EmployeePasswordRequiredException(String message) {
        super(message);
    }
}
