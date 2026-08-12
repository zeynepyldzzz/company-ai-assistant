package com.company.assistant.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST/PUT /admin/employees govdesi.
 *
 * <p>A-29 (#178): {@code password} alani KALDIRILDI. Admin artik hicbir yolla sifre
 * belirlemiyor — olusturmada sistem gecici bir sifre uretiyor, sifirlama icin ayri bir uc
 * var (POST /admin/employees/{id}/reset-password).
 *
 * <p>A-32 (#188): {@code officeStatus} alani KALDIRILDI. Ofis durumu bugunun calisma
 * duzeninden turetiliyor; admin'in yazdigi deger hicbir yerde okunmuyor olacakti. Ekranda
 * duran ama etkisi olmayan bir alan, admin'in veriyi girdigini sanmasina yol acar —
 * bu issue'nun cozdugu celiskinin (kolon "Ofiste", plan "REMOTE") kaynagi da tam olarak oydu.
 *
 * <p>Gerekce: admin'in bir calisanin sifresini bilmesi, sifreyi kimlik dogrulama araci
 * olmaktan cikarir. Iki yol (admin girer / sistem uretir) birakildiginda ikisinin de sonucu
 * gecici sifre oluyordu; ikinci yolu tutup birincisini kaldirmak hem kodu hem arayuzu
 * sadelestiriyor. Kural tek cumleye indi: sifreyi yalnizca kullanicinin kendisi belirler.
 */
public record AdminEmployeeRequest(
        /**
         * A-35 (#196): ad ve soyad ayri alanlar. Soyad YENI kayitlarda zorunlu; entity'de
         * nullable olmasinin tek sebebi V50 oncesinde tek kelimeli kaydedilmis calisanlar
         * (hepsi test hesabi). Eski veri esnek, yeni veri kati.
         */
        // A-44 (#219): sinirlarin kaynagi migration'daki kolon genisligi
        // (V50: first_name/last_name VARCHAR(100), V1: email VARCHAR(150), phone VARCHAR(30)).
        @NotBlank @Size(max = 100, message = "Ad 100 karakteri aşamaz") String firstName,
        @NotBlank @Size(max = 100, message = "Soyad 100 karakteri aşamaz") String lastName,

        @NotBlank @Email @Size(max = 150, message = "E-posta 150 karakteri aşamaz") String email,
        @Size(max = 30, message = "Telefon 30 karakteri aşamaz") String phone,

        /**
         * A-30 (#185): departman ZORUNLU.
         *
         * <p>Kisitlama uygulama katmaninda; {@code employee.department_id} kolonuna NOT NULL
         * KONULAMAZ, cunku mevcut satirlarda NULL var ve migration ayaga kalkmazdi. Once
         * o satirlara departman atanmali, NOT NULL ayri bir issue.
         *
         * <p>Not: bu kural, departmansiz calisanin gorunmemesinin CARESI DEGIL — o hata
         * EmployeeRepository.search()'teki ortuk INNER JOIN'di ve ayrica duzeltildi. Buradaki
         * amac yeni bos kayit uretilmesini durdurmak.
         */
        @NotNull(message = "Departman seçilmelidir.")
        Integer departmentId,

        Integer roleId) {
}
