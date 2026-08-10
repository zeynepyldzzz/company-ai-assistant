package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

/**
 * A-34 (#194): rehber aramasinin KELIME BASI eslesmesi ve deterministik siralamasi.
 *
 * <p>Gercek sorgu davranisini test ediyor, mock'lanmis bir servisi degil — kural JPQL'in
 * icinde yasiyor. Test gercek Postgres'e baglaniyor ({@code replace = NONE}) ve
 * {@code employee} tablosu PAYLASIMLI: uygulamanin kendi verisi de orada duruyor. Bu
 * yuzden butun kayitlar benzersiz uydurma isimlerle ekleniyor ve assertion'lar yalnizca
 * o isimlere bakiyor; "a" gibi bir terimle arama yapip tum tabloyu saymak, gercek veri
 * degistiginde kirilan bir test olurdu (bkz. FeedbackAnonymityTest'in ayni tuzagi).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeSearchMatchingIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    // Gercek veriyle cakismayacak kadar uydurma. "Zyx" ile baslayan bir calisan yok.
    private static final String ADI = "Zyxwvu";
    private static final String SOYADI = "Qponml";

    @BeforeEach
    void seed() {
        persist(ADI, SOYADI);
        persist(SOYADI, ADI);
    }

    private void persist(String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail((firstName + "." + lastName).toLowerCase() + "@test.local");
        employee.setPasswordHash("$2a$10$testhashtesthashtesthashtesthashtesthashtes");
        employee.setActive(true);
        entityManager.persist(employee);
        entityManager.flush();
    }

    private List<String> searchNames(String term) {
        return employeeRepository
                .search(term, null, null, LocalDate.now(), "monday", PageRequest.of(0, 50))
                .getContent().stream()
                .map(Employee::getName)
                .toList();
    }

    // Adin basindan arama: her iki kayit da eslesir (birinde ad, digerinde soyad).
    @Test
    void kelimeBasindanAramaAdVeSoyadiBulur() {
        assertThat(searchNames("zyxw"))
                .containsExactlyInAnyOrder(ADI + " " + SOYADI, SOYADI + " " + ADI);
    }

    /**
     * Asil duzeltme: kelime ORTASINDAN eslesme artik sonuc vermiyor. Onceden {@code %a%}
     * yazimi yuzunden "yxwv" gibi bir parca da tum kayitlari getiriyordu.
     */
    @Test
    void kelimeOrtasindanAramaSonucVermez() {
        assertThat(searchNames("yxwv")).isEmpty();
    }

    // Soyadiyla arama calismaya devam etmeli; sadece "bastan eslesme" yapsaydik kirilirdi.
    @Test
    void soyadiylaAramaCalisir() {
        assertThat(searchNames("qponml"))
                .containsExactlyInAnyOrder(ADI + " " + SOYADI, SOYADI + " " + ADI);
    }

    /**
     * Siralama sorguda tanimli olmali. Onceden ne sorguda ne PageRequest'te siralama vardi;
     * Postgres sirasiz sorguda satir sirasini garanti etmedigi icin LIMIT/OFFSET ile ayni
     * kisi iki sayfada birden cikabilirdi.
     *
     * <p>A-35 (#196): siralama artik SOYADA gore — rehberlerin klasik davranisi. Iki kayit
     * ayni iki kelimenin yer degistirmis hali oldugu icin bu test, siralamanin gercekten
     * soyadi okudugunu gosterir: tam ada gore sirali olsalardi sonuc ters cikardi.
     */
    @Test
    void sonuclarSoyadaGoreSirali() {
        List<String> lastNames = employeeRepository
                .search("zyxw", null, null, LocalDate.now(), "monday", PageRequest.of(0, 50))
                .getContent().stream()
                .map(Employee::getLastName)
                .toList();

        assertThat(lastNames).isSorted();
    }
}
