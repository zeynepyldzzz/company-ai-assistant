package com.company.assistant.directory;

/**
 * A-29 (#178): calisan olusturma yaniti.
 *
 * <p>{@code generatedPassword} YALNIZCA sistem sifre urettiginde dolar ve YALNIZCA bu
 * yanitta doner — admin onu calisana iletir, sonra bir daha hicbir yerden okunamaz
 * (veritabaninda yalnizca hash var).
 *
 * <p>Ayri bir tip olmasinin sebebi: {@link EmployeeResponse} listeleme ve detay uclarinda
 * da kullaniliyor. Sifre alanini oraya eklemek, alanin ileride yanlislikla baska bir uctan
 * sizmasi riskini dogururdu. Tip ayrimi bu riski yapisal olarak kapatiyor.
 */
public record AdminEmployeeCreateResponse(EmployeeResponse employee, String generatedPassword) {
}
