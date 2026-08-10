package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * A-35 (#196): {@code Employee.getName()} artik bir kolon degil, ad+soyaddan TURETILIYOR.
 *
 * <p>Bu metodun dogru calismasi genis bir yuzeyi tasiyor: rehber kartlari, chatbot
 * resolver'lari, DTO'lar ve PDF uretimi hepsi {@code getName()} okuyor. V49 ile kolon
 * kaldirildigi icin burada bir hata, tum bu noktalarda ayni anda gorunur.
 */
class EmployeeFullNameTest {

    private Employee employee(String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        return employee;
    }

    @Test
    void adVeSoyadBirlestirilir() {
        assertThat(employee("Ayse", "Kaya").getName()).isEqualTo("Ayse Kaya");
    }

    /**
     * V49 oncesinde tek kelimeli kaydedilmis calisanlar var (hepsi test hesabi) ve
     * {@code last_name} onlarda NULL. Naif birlestirme "TestAdmin2 null" ya da sondaki
     * bosluguyla "TestAdmin2 " uretirdi; ikincisi arayuzde gorunmez ama esitlik
     * karsilastirmalarini sessizce kaydirir.
     */
    @Test
    void soyadiYoksaSondaBoslukKalmaz() {
        assertThat(employee("TestAdmin2", null).getName()).isEqualTo("TestAdmin2");
    }

    @Test
    void soyadiBosStringOlsaDaSondaBoslukKalmaz() {
        assertThat(employee("TestAdmin2", "   ").getName()).isEqualTo("TestAdmin2");
    }
}
