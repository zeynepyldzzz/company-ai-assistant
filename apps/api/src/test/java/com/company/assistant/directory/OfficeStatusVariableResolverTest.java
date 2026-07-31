package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.common.PagedResponse;

/**
 * A-14 (#115): "kimler ofiste" rehber kaynakli yanit. Odak, issue'nun is kurallari:
 * departman kapsami, durum filtresi, veri yoklugu ve departmansiz kullanici.
 */
@ExtendWith(MockitoExtension.class)
class OfficeStatusVariableResolverTest {

    private static final int EMPLOYEE_ID = 7;

    @Mock
    private DirectoryService directoryService;
    @Mock
    private DepartmentService departmentService;

    private OfficeStatusVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OfficeStatusVariableResolver(directoryService, departmentService);
        // Mock'lar disarida kurulur: when(...) argumani icinde mock stub'lamak Mockito'da
        // "UnfinishedStubbing" hatasi verir.
        List<DepartmentResponse> departments = List.of(
                department("Insan Kaynaklari"),
                department("Bilgi Teknolojileri"),
                department("Satis ve Pazarlama"));
        lenient().when(departmentService.searchDepartments(isNull(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(departments, 0, 100, 3));
    }

    @Test
    void ucuncuSahisIpucuAyirtEdilir() {
        assertThat(resolver.isThirdPersonQuestion("kimler ofiste")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("kim ofiste")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("kimlerin izni var")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("bugun ofiste miyim")).isFalse();
    }

    // A-20 (#139): "kim" tasimayan ucuncu sahis kaliplari. Bunlar eskiden kullanicinin
    // KENDI haftalik planini donduruyordu — soru baskalari hakkindayken.
    @Test
    void kimIcermeyenUcuncuSahisKaliplariDaAyirtEdilir() {
        assertThat(resolver.isThirdPersonQuestion("ofiste olan kisiler")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("sirkette olan kisilerin listesi")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("calisanlar nerede")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("bugun ofiste olanlar")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("personel durumu nedir")).isTrue();
        assertThat(resolver.isThirdPersonQuestion("herkes ofiste mi")).isTrue();
    }

    // Birinci sahis sorulari yeni kaliplarla kirilmamali (nobetci).
    @Test
    void birinciSahisSorulariUcuncuSahisSayilmaz() {
        assertThat(resolver.isThirdPersonQuestion("bu hafta hangi gunler ofisteyim")).isFalse();
        assertThat(resolver.isThirdPersonQuestion("calisma duzenim nedir")).isFalse();
        assertThat(resolver.isThirdPersonQuestion("yarin uzaktan miyim")).isFalse();
    }

    // "kimlik" / "kimse" gibi kelimeler ucuncu sahis sorusu degildir.
    @Test
    void kimIceren_ama_ucuncuSahisOlmayanKelimelerEslesmez() {
        assertThat(resolver.isThirdPersonQuestion("kimlik dogrulama nasil yapilir")).isFalse();
        assertThat(resolver.isThirdPersonQuestion("kimse yok mu")).isFalse();
    }

    @Test
    void departmanBelirtilmezseKullanicininDepartmaniListelenir() {
        whenOwnDepartment("Bilgi Teknolojileri");
        whenSearch("Bilgi Teknolojileri", "Ofiste", List.of(employee("Ayse Kaya"), employee("Gizem Arslan")), 2);
        whenCompanyTotal("Ofiste", 5);

        String reply = resolver.resolve("kimler ofiste", EMPLOYEE_ID);

        assertThat(reply)
                .contains("Bilgi Teknolojileri departmanında ofiste görünenler")
                .contains("• Ayse Kaya")
                .contains("• Gizem Arslan")
                .contains("Şirket genelinde 5 kişi ofiste görünüyor.");
    }

    @Test
    void mesajdaGecenDepartmanKullanicininkiniEzer() {
        whenSearch("Satis ve Pazarlama", "Ofiste", List.of(employee("Emre Koc")), 1);
        whenCompanyTotal("Ofiste", 5);

        String reply = resolver.resolve("satis departmaninda kimler ofiste", EMPLOYEE_ID);

        assertThat(reply).contains("Satis ve Pazarlama departmanında").contains("Emre Koc");
    }

    @Test
    void uzaktanSorusuDogruFiltreyleCalisir() {
        whenOwnDepartment("Bilgi Teknolojileri");
        whenSearch("Bilgi Teknolojileri", "Uzaktan", List.of(employee("Mehmet Demir")), 1);
        whenCompanyTotal("Uzaktan", 3);

        String reply = resolver.resolve("kimler uzaktan calisiyor", EMPLOYEE_ID);

        assertThat(reply)
                .contains("uzaktan çalışanlar")
                .contains("Mehmet Demir")
                .contains("Şirket genelinde 3 kişi uzaktan görünüyor.");
    }

    @Test
    void izindeSorusuDogruFiltreyleCalisir() {
        whenOwnDepartment("Bilgi Teknolojileri");
        whenSearch("Bilgi Teknolojileri", "Izinde", List.of(employee("Burak Yildiz")), 1);
        whenCompanyTotal("Izinde", 2);

        String reply = resolver.resolve("kimler izinde", EMPLOYEE_ID);

        assertThat(reply).contains("izinde olanlar").contains("Burak Yildiz");
    }

    @Test
    void departmanBosSaKimseYokMesajiDoner() {
        whenOwnDepartment("Bilgi Teknolojileri");
        whenSearch("Bilgi Teknolojileri", "Ofiste", List.of(), 0);
        whenCompanyTotal("Ofiste", 5);

        String reply = resolver.resolve("kimler ofiste", EMPLOYEE_ID);

        assertThat(reply)
                .contains("ofiste görünen kimse yok")
                .contains("Şirket genelinde 5 kişi");
    }

    // Departmani olmayan calisan kayitlari var; rastgele departman secilmemeli.
    @Test
    void kullanicininDepartmaniYoksaDepartmanSorulur() {
        whenOwnDepartment(null);
        whenCompanyTotal("Ofiste", 5);

        String reply = resolver.resolve("kimler ofiste", EMPLOYEE_ID);

        assertThat(reply)
                .contains("Hangi departmanı sorduğunu yazarsan")
                .contains("Şirket genelinde 5 kişi ofiste görünüyor.");
    }

    @Test
    void listeUstSiniriAsilirsaKalanSayiBelirtilir() {
        whenOwnDepartment("Bilgi Teknolojileri");
        whenSearch("Bilgi Teknolojileri", "Ofiste", List.of(employee("Ayse Kaya")), 30);
        whenCompanyTotal("Ofiste", 40);

        String reply = resolver.resolve("kimler ofiste", EMPLOYEE_ID);

        assertThat(reply).contains("ve 29 kişi daha");
    }

    // --- A-20 (#139): sirket geneli kapsam ---

    @Test
    void sirketGeneliSorulursaKullanicininDepartmaniylaSinirlanmaz() {
        whenCompanySearch("Ofiste",
                List.of(employee("Ayse Kaya", "Bilgi Teknolojileri"),
                        employee("Emre Koc", "Satis ve Pazarlama")), 2);

        String reply = resolver.resolve("sirkette olan kisiler", EMPLOYEE_ID);

        assertThat(reply)
                .contains("Şirket genelinde ofiste görünenler (2 kişi)")
                .contains("• Ayse Kaya — Bilgi Teknolojileri")
                .contains("• Emre Koc — Satis ve Pazarlama")
                .doesNotContain("departmanında");
    }

    // Departman adi acikca gectiginde dar kapsam kazanir: "tum" kelimesi sirket geneline
    // kaydirmamali, aksi halde "tum satis departmani" sorusu 40 kisilik liste doner.
    @Test
    void acikDepartmanAdiSirketGeneliIpucunuEzer() {
        whenSearch("Satis ve Pazarlama", "Ofiste", List.of(employee("Emre Koc")), 1);
        whenCompanyTotal("Ofiste", 5);

        String reply = resolver.resolve("tum satis departmaninda kimler ofiste", EMPLOYEE_ID);

        assertThat(reply).contains("Satis ve Pazarlama departmanında").contains("Emre Koc");
    }

    @Test
    void sirketGeneliBosSaAcikMesajDoner() {
        whenCompanySearch("Izinde", List.of(), 0);

        String reply = resolver.resolve("sirkette kimler izinde", EMPLOYEE_ID);

        assertThat(reply).isEqualTo("Şirket genelinde izinde kimse yok.");
    }

    @Test
    void sirketGeneliListesiUstSiniriAsarsaKalanSayiBelirtilir() {
        whenCompanySearch("Ofiste", List.of(employee("Ayse Kaya", "Bilgi Teknolojileri")), 30);

        String reply = resolver.resolve("tum sirkette kimler ofiste", EMPLOYEE_ID);

        assertThat(reply).contains("(30 kişi)").contains("ve 29 kişi daha");
    }

    private void whenOwnDepartment(String departmentName) {
        EmployeeResponse response = mock(EmployeeResponse.class);
        when(response.getDepartmentName()).thenReturn(departmentName);
        when(directoryService.getEmployeeById(EMPLOYEE_ID)).thenReturn(response);
    }

    private void whenSearch(String department, String status, List<EmployeeResponse> data, long total) {
        when(directoryService.searchEmployees(isNull(), eq(department), eq(status), eq(0), anyInt()))
                .thenReturn(new PagedResponse<>(data, 0, 25, total));
    }

    private void whenCompanyTotal(String status, long total) {
        when(directoryService.searchEmployees(isNull(), isNull(), eq(status), eq(0), eq(1)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 1, total));
    }

    // Sirket geneli LISTE sorgusu: departman filtresi yok, sayfa boyutu MAX_NAMES (25).
    // whenCompanyTotal ile karismaz — o eq(1) sayfa boyutuyla yalnizca sayim yapar.
    private void whenCompanySearch(String status, List<EmployeeResponse> data, long total) {
        when(directoryService.searchEmployees(isNull(), isNull(), eq(status), eq(0), eq(25)))
                .thenReturn(new PagedResponse<>(data, 0, 25, total));
    }

    private EmployeeResponse employee(String name) {
        return employee(name, null);
    }

    private EmployeeResponse employee(String name, String departmentName) {
        EmployeeResponse response = mock(EmployeeResponse.class);
        lenient().when(response.getName()).thenReturn(name);
        lenient().when(response.getDepartmentName()).thenReturn(departmentName);
        return response;
    }

    private DepartmentResponse department(String name) {
        DepartmentResponse response = mock(DepartmentResponse.class);
        lenient().when(response.getName()).thenReturn(name);
        return response;
    }
}
