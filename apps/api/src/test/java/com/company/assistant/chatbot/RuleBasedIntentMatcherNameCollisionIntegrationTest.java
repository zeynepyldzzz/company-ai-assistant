package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.company.assistant.common.TurkishText;
import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

/**
 * A-38 (#207): BEKCI — rehberdeki isimler kural katmaninin eleme listeleriyle cakismamali.
 *
 * <p><b>Neden gerekli:</b> {@code RuleBasedIntentMatcher} isim adayi ararken bazi kelimeleri
 * eliyor — jenerik kelimeler ("calisan", "kisi"), ay adlari, hitaplar ("bey", "abla") ve
 * sahis zamirleri ("ben", "sen"). Eleme TAM kelime eslesmesiyle calisiyor ve Turkce
 * karakterler ASCII'ye katlaniyor, yani <b>soyadi "Şen" olan biri "sen" zamiriyle ayni
 * tokeni uretiyor</b>. Boyle bir kayit eklendigi gun o kisi kural katmaninda bulunamaz —
 * hicbir hata vermeden, sessizce.
 *
 * <p>Bu, A-38'de elle sorulan bir soruydu ("Şen soyadlı biri olursa ne olur?"). Elle
 * yapilan bir kontrol yalnizca o gunku rehberi kapsar; ise alim her ay oluyor ve kimse bu
 * listeyi hatirlamayacak. Bekci CI'da kirilarak haber verir.
 *
 * <p><b>Kirildiginda ne yapmali:</b> genellikle dogru hamle o kelimeyi listeden CIKARMAK.
 * Iki hatanin bedeli simetrik degil — gercek bir calisanin bulunamamasi, siradan bir
 * kelimenin bazen yanlis tetiklemesinden pahali. Cakisma bilerek kabul ediliyorsa
 * {@link #KABUL_EDILEN_CAKISMALAR} listesine gerekcesiyle eklenmeli.
 *
 * <p><b>Kapsam:</b> yalnizca CALISAN adlari. Departman/durak/hat adlari icin ayri bir risk
 * var ama farkli bir eleme yolundan geciyor ({@code mentionsName}, orada dort harf alt
 * siniri da devrede); A-38'de elle dogrulandi, bekcisi ayri bir is.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RuleBasedIntentMatcherNameCollisionIntegrationTest {

    /**
     * Bilerek kabul edilmis cakismalar.
     *
     * <p>{@code calisan}: rehberde soyadi "Calisan" olan bir test hesabi var (Test Calisan).
     * A-38'de olculdu — "calisan" isim adayi sayildiginda "çarşamba günü uzaktan çalışan kaç
     * kişi var" sorusu tek bir kisinin kartina kayiyordu. Jenerik kelimenin HER mesajda
     * yanlis tetiklemesi, o soyadin yalnizca soyadiyla aranamamasindan pahali; kayit adiyla
     * ("test") hala bulunuyor.
     */
    private static final Set<String> KABUL_EDILEN_CAKISMALAR = Set.of("calisan");

    /** Rehber bundan buyurse test kor kalir; asagida ayrica dogrulaniyor. */
    private static final int PAGE_SIZE = 1000;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void rehberdekiIsimlerElemeListesiyleCakismaz() {
        List<String> reserved = RuleBasedIntentMatcher.reservedNameWords();

        List<String> cakismalar = activeEmployees().stream()
                .flatMap(employee -> nameWords(employee)
                        .filter(reserved::contains)
                        .filter(word -> !KABUL_EDILEN_CAKISMALAR.contains(word))
                        .map(word -> "\"" + word + "\" -> " + employee.getName()))
                .distinct()
                .toList();

        assertThat(cakismalar)
                .as("""
                    Rehberdeki bir isim, kural katmaninin eleme listesinde geciyor. Bu kisi \
                    chatbot'ta ADIYLA BULUNAMAZ (kural katmani kelimeyi isim adayi saymaz) \
                    ve bu sessiz bir kayiptir.

                    Cozum: kelimeyi RuleBasedIntentMatcher.NON_NAME_WORDS / GENERIC_WORDS \
                    listesinden cikar. Cakisma bilerek kabul ediliyorsa bu testteki \
                    KABUL_EDILEN_CAKISMALAR listesine GEREKCESIYLE ekle.""")
                .isEmpty();
    }

    /** Bekci sayfa disinda kalan kayitlari goremez; sessizce kor kalmasin. */
    @Test
    void bekciTumRehberiKapsar() {
        assertThat(page().getTotalElements())
                .as("Rehber %d kaydi asti; PAGE_SIZE buyutulmeli, aksi halde bekci "
                        + "sayfa disindaki isimleri hic kontrol etmiyor demektir", PAGE_SIZE)
                .isLessThanOrEqualTo(PAGE_SIZE);
    }

    private List<Employee> activeEmployees() {
        return page().getContent();
    }

    // search(null, ...) tum AKTIF calisanlari verir (sorgu e.active = true iceriyor).
    // office filtresi null oldugu icin weekStart/dayOfWeek degerleri sorguda kullanilmiyor.
    private Page<Employee> page() {
        return employeeRepository.search(
                null, null, null, LocalDate.now(), "monday", PageRequest.of(0, PAGE_SIZE));
    }

    /**
     * Adin kural katmanindaki haliyle AYNI parcalanmasi: once ASCII'ye katlanir (Turkce
     * karakterler burada kaybolur — "Şen" ve "sen" ayni tokene duser, riskin kaynagi bu),
     * sonra alfanumerik olmayan her sey ayirici sayilir.
     */
    private Stream<String> nameWords(Employee employee) {
        String lastName = employee.getLastName() == null ? "" : employee.getLastName();
        return Stream.of(TurkishText.foldToAscii(employee.getFirstName() + " " + lastName)
                        .split("[^a-z0-9]+"))
                .filter(word -> !word.isEmpty());
    }
}
