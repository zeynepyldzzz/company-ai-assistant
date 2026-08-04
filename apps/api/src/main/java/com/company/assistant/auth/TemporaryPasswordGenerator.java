package com.company.assistant.auth;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

/**
 * A-29 (#178): yeni calisan hesaplari icin gecici sifre uretir.
 *
 * <p>Uretilen sifre admin'e YALNIZCA olusturma yanitinda bir kez gosterilir; veritabaninda
 * yalnizca hash saklanir, duz metin hicbir yerde tutulmaz ve loglanmaz.
 *
 * <p><b>Alfabede karisan karakterler yok</b> (0/O, 1/l/I). Bu sifre ekrandan okunup elle
 * iletilecek — admin calisana soyleyecek ya da yazacak — dolayisiyla "1 mi l mi" sorusu
 * pratik bir sorun. Guclulukten kaybedilen az miktar, uzunluk ile fazlasiyla telafi ediliyor.
 *
 * <p>Her karakter sinifindan en az bir tane garanti edilir; aksi halde rastgelelik nadiren de
 * olsa yalnizca harflerden olusan bir sifre uretebilir ve sifre kurallarini saglamayabilir.
 */
@Service
public class TemporaryPasswordGenerator {

    private static final String DIGITS = "23456789";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String SYMBOLS = "!?*-+";
    private static final String ALL = DIGITS + UPPER + LOWER + SYMBOLS;

    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        password.append(pick(DIGITS))
                .append(pick(UPPER))
                .append(pick(LOWER))
                .append(pick(SYMBOLS));
        while (password.length() < LENGTH) {
            password.append(pick(ALL));
        }
        return shuffle(password);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    /** Garanti edilen karakterler bastaki sabit sirada kalmasin diye karistirilir. */
    private String shuffle(StringBuilder password) {
        for (int i = password.length() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = password.charAt(i);
            password.setCharAt(i, password.charAt(j));
            password.setCharAt(j, tmp);
        }
        return password.toString();
    }
}
