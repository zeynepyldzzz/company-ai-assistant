package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.common.PagedResponse;

/**
 * A-15 (#117): kisi arama. Odak, issue'nun is kurallari: ad/soyad/tam ad eslesmesi,
 * coklu eslesmede netlestirme, eslesmeme ve NULL alanlar.
 */
@ExtendWith(MockitoExtension.class)
class DirectoryVariableResolverTest {

    @Mock
    private DirectoryService directoryService;

    private DirectoryVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DirectoryVariableResolver(directoryService);
        // Varsayilan: hicbir kelime kimseyle eslesmez; testler kendi eslesmesini ekler.
        lenient().when(directoryService.searchEmployees(anyString(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(empty());
    }

    @Test
    void rehberDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu", "Ayşe Kaya kimdir")).isEmpty();
    }

    // A-20 (#139): sorulan alan belliyse yanit TEK SATIR. Eskiden dort satirlik kart
    // donuyordu ve kullanici kendi sorusunun cevabini kartin icinde ariyordu.
    @Test
    void dahiliSorusuYalnizcaTelefonDoner() {
        whenSearch("ayse", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Ofiste"));
        whenSearch("kaya", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Ofiste"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Ayşe Kaya'nın dahilisi kaç");

        assertThat(vars.get("kisi_bilgisi"))
                .isEqualTo("Ayse Kaya — telefon: 1234");
    }

    @Test
    void ofisDurumuSorusuTekCumleDoner() {
        whenSearch("ayse", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Uzaktan"));
        whenSearch("kaya", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Uzaktan"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Ayşe Kaya ofiste mi");

        assertThat(vars.get("kisi_bilgisi")).isEqualTo("Ayse Kaya şu an Uzaktan görünüyor.");
    }

    // "nerede calisiyor" departman sorusudur; tek basina "nerede" ofis durumu.
    @Test
    void neredeCalisiyorDepartmanDoner() {
        whenSearch("demir", employee(2, "Mehmet Demir", "Muhasebe ve Finans", "5555", "m@x.com", "Ofiste"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Mehmet Demir nerede çalışıyor");

        assertThat(vars.get("kisi_bilgisi")).isEqualTo("Mehmet Demir — departman: Muhasebe ve Finans");
    }

    @Test
    void neredeSorusuOfisDurumuDoner() {
        whenSearch("demir", employee(2, "Mehmet Demir", "Muhasebe ve Finans", "5555", "m@x.com", "Ofiste"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Mehmet Demir nerede");

        assertThat(vars.get("kisi_bilgisi")).isEqualTo("Mehmet Demir şu an Ofiste görünüyor.");
    }

    // Alan bos oldugunda soru cevapsiz kalmaz: elde ne varsa gosterilir.
    @Test
    void sorulanAlanBossaTamKartaDusulur() {
        whenSearch("demir", employee(2, "Mehmet Demir", "Muhasebe ve Finans", null, "m@x.com", "Ofiste"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Mehmet Demir'in dahilisi");

        assertThat(vars.get("kisi_bilgisi"))
                .contains("kayıtlı telefon bilgisi yok")
                .contains("E-posta: m@x.com");
    }

    // Acik uclu soruda alan tespit edilemez — tam kart dogru davranistir.
    @Test
    void alanBelirtilmeyenSoruTamKartDoner() {
        whenSearch("ayse", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Ofiste"));
        whenSearch("kaya", employee(1, "Ayse Kaya", "Bilgi Teknolojileri", "1234", "ayse@x.com", "Ofiste"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Ayşe Kaya kimdir");

        assertThat(vars.get("kisi_bilgisi"))
                .contains("Telefon: 1234")
                .contains("Departman: Bilgi Teknolojileri")
                .contains("Ofis durumu: Ofiste");
    }

    @Test
    void soyadIleAramaCalisir() {
        whenSearch("demir", employee(2, "Mehmet Demir", "Muhasebe ve Finans", "5555", "m@x.com", "Uzaktan"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Demir'in e-postası nedir");

        assertThat(vars.get("kisi_bilgisi")).contains("Mehmet Demir").contains("m@x.com");
    }

    // Tam ad eslesmesi, yalnizca soyadi tutan adayin onune gecmeli.
    @Test
    void tamAdEslesmesiTekilEslesmeninOnuneGecer() {
        whenSearch("ayse", employee(1, "Ayse Kaya", "BT", "1234", "ayse@x.com", "Ofiste"));
        whenSearch("kaya", employee(1, "Ayse Kaya", "BT", "1234", "ayse@x.com", "Ofiste"),
                employee(3, "Burak Kaya", "Satis", "9999", "burak@x.com", "Izinde"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Ayşe Kaya hangi departmanda");

        assertThat(vars.get("kisi_bilgisi")).contains("Ayse Kaya").doesNotContain("Burak Kaya");
    }

    // Ayni skoru paylasan birden fazla kisi varsa rastgele secim YAPILMAZ.
    @Test
    void cokluEslesmedeNetlestirmeSorusuDoner() {
        whenSearch("kaya", employee(1, "Ayse Kaya", "BT", "1234", "ayse@x.com", "Ofiste"),
                employee(3, "Burak Kaya", "Satis", "9999", "burak@x.com", "Izinde"));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Kaya'nın dahilisi kaç");

        assertThat(vars.get("kisi_bilgisi"))
                .contains("Birden fazla kişi eşleşti")
                .contains("Ayse Kaya")
                .contains("Burak Kaya");
    }

    @Test
    void eslesmeYoksaNetMesajDoner() {
        Map<String, String> vars = resolver.resolve("rehber_kisi", "Zeynep Yildirim kimdir");

        assertThat(vars.get("kisi_bilgisi")).contains("rehberde bulamadım");
    }

    // "bir çalışanın telefon numarasını bulmak istiyorum" — isim adayi kelime yok.
    @Test
    void isimGecmiyorsaKimiAradiginiSorar() {
        Map<String, String> vars = resolver.resolve("rehber_kisi", "dahili numara bilgisi");

        assertThat(vars.get("kisi_bilgisi")).contains("Kimi aradığını yazarsan");
    }

    // Aktif calisanlar arasinda departmani/telefonu olmayan kayitlar var.
    @Test
    void nullAlanlarHamNullOlarakBasilmaz() {
        whenSearch("calisan", employee(4, "Test Calisan", null, null, "test@x.com", null));

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Test Calisan bilgileri");

        assertThat(vars.get("kisi_bilgisi"))
                .contains("Departman: belirtilmemiş")
                .contains("Telefon: belirtilmemiş")
                .contains("Ofis durumu: belirtilmemiş")
                .doesNotContain("null");
    }

    // Rol bilgisi EmployeeResponse'ta var (C-11/#85) ama chatbot basmaz.
    @Test
    void rolBilgisiYanittaYerAlmaz() {
        EmployeeResponse employee = employee(1, "Ayse Kaya", "BT", "1234", "ayse@x.com", "Ofiste");
        lenient().when(employee.getRoleName()).thenReturn("hr_admin");
        whenSearch("kaya", employee);

        Map<String, String> vars = resolver.resolve("rehber_kisi", "Kaya kimdir");

        assertThat(vars.get("kisi_bilgisi")).doesNotContain("hr_admin");
    }

    private void whenSearch(String token, EmployeeResponse... employees) {
        when(directoryService.searchEmployees(eq(token), isNull(), isNull(), eq(0), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(employees), 0, 25, employees.length));
    }

    private PagedResponse<EmployeeResponse> empty() {
        return new PagedResponse<>(List.of(), 0, 25, 0);
    }

    private EmployeeResponse employee(Integer id, String name, String department,
                                      String phone, String email, String officeStatus) {
        EmployeeResponse response = mock(EmployeeResponse.class);
        lenient().when(response.getId()).thenReturn(id);
        lenient().when(response.getName()).thenReturn(name);
        lenient().when(response.getDepartmentName()).thenReturn(department);
        lenient().when(response.getPhone()).thenReturn(phone);
        lenient().when(response.getEmail()).thenReturn(email);
        lenient().when(response.getOfficeStatus()).thenReturn(officeStatus);
        return response;
    }
}
