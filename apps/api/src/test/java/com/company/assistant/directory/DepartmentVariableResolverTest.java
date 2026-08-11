package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.schedule.StatusDayResolver;
import com.company.assistant.schedule.TodayStatusService;

/**
 * A-18 (#127): departman iletisim bilgisi. Odak: departman eslestirme, anlasilmayan
 * departmanda netlestirme ve NULL alanlar.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentVariableResolverTest {

    /** Sabit referans: gun cikarimi takvime bagli olmasin (bkz. StatusDayResolverTest). */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 1).with(DayOfWeek.MONDAY);

    @Mock
    private DepartmentService departmentService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private TodayStatusService todayStatusService;

    private DepartmentVariableResolver resolver;

    @BeforeEach
    void setUp() {
        // StatusDayResolver gercegi veriliyor (bkz. OfficeStatusVariableResolverTest'teki
        // ayni gerekce); takvim bagimliligi TodayStatusService mock'uyla kesiliyor.
        resolver = new DepartmentVariableResolver(
                departmentService, directoryService, new StatusDayResolver(todayStatusService));
        lenient().when(todayStatusService.today()).thenReturn(TODAY);
        lenient().when(todayStatusService.currentWeekStart()).thenReturn(TODAY);
    }

    @Test
    void departmanDisiIntentBosDoner() {
        assertThat(resolver.resolve("rehber_kisi", "Ayşe Kaya kimdir")).isEmpty();
    }

    @Test
    void departmanAdiGecinceIletisimBilgisiDoner() {
        seedDepartments();

        String reply = resolver.resolve("rehber_departman", "muhasebeye nasıl ulaşırım")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Muhasebe ve Finans")
                .contains("Yönetici: Elif Sahin")
                .contains("Telefon: 1005")
                .contains("elif@company.com");
    }

    @Test
    void turkceKaraktersizGirisDeEslesir() {
        seedDepartments();

        assertThat(resolver.resolve("rehber_departman", "bilgi teknolojileri kime bagli")
                .get("departman_bilgisi")).contains("Bilgi Teknolojileri");
    }

    // Rastgele departman secilmemeli; kullaniciya mevcut departmanlar gosterilip sorulur.
    @Test
    void departmanAnlasilmazsaListeSunulur() {
        seedDepartments();

        // "bilgisi"/"iletisim" GENERIC_WORDS'te oldugu icin hicbir departmani secmemeli.
        String reply = resolver.resolve("rehber_departman", "departman iletişim bilgisi lazım")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Hangi departmanı sorduğunu yazarsan")
                .contains("• Muhasebe ve Finans")
                .contains("• Bilgi Teknolojileri");
    }

    @Test
    void yoneticisiOlmayanDepartmanNullBasmaz() {
        // Mock disarida kurulur: when(...) argumani icinde mock stub'lamak UnfinishedStubbing verir.
        DepartmentResponse withoutManager = department("Bilgi Teknolojileri", null, null, null, null);
        when(departmentService.searchDepartments(isNull(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(withoutManager), 0, 100, 1));

        String reply = resolver.resolve("rehber_departman", "bilgi teknolojileri kime bağlı")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Yönetici: belirtilmemiş")
                .contains("Telefon: belirtilmemiş")
                .doesNotContain("null");
    }

    @Test
    void hicDepartmanYoksaNetMesajDoner() {
        when(departmentService.searchDepartments(isNull(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 100, 0));

        assertThat(resolver.resolve("rehber_departman", "muhasebe").get("departman_bilgisi"))
                .isEqualTo("Sistemde tanımlı departman bulunmuyor.");
    }

    // --- A-25 (#169): departman calisan listesi ---

    // Olculdu: "Muhasebe çalışanları" 0.644 ile intent_bulunamadi donuyordu; eslesse bile
    // eski davranis yonetici bilgisi dondururdu, liste degil.
    @Test
    void calisanListesiIstenirseKisilerListelenir() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans",
                List.of(employee("Elif Sahin", "Ofiste"), employee("Burak Yildiz", "Izinde")), 2);

        String reply = resolver.resolve("rehber_departman", "muhasebe çalışanları")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Muhasebe ve Finans departmanında 2 kişi çalışıyor")
                .contains("• Elif Sahin — Ofiste")
                .contains("• Burak Yildiz — Izinde")
                .doesNotContain("Yönetici:");
    }

    @Test
    void kimlerVarSorusuDaListeDoner() {
        seedDepartments();
        whenEmployeeSearch("Bilgi Teknolojileri", List.of(employee("Emre Koc", "Uzaktan")), 1);

        assertThat(resolver.resolve("rehber_departman", "bilgi teknolojilerinde kimler var")
                .get("departman_bilgisi")).contains("Emre Koc");
    }

    // Alan ipucu yoksa DAVRANIS DEGISMEZ: iletisim bilgisi doner (A-18 regresyonu).
    @Test
    void listeIpucuYoksaIletisimBilgisiDoner() {
        seedDepartments();

        String reply = resolver.resolve("rehber_departman", "muhasebe departmanı yetkilisi kim")
                .get("departman_bilgisi");

        assertThat(reply).contains("Yönetici: Elif Sahin").doesNotContain("kişi çalışıyor");
    }

    @Test
    void bosDepartmandaAcikMesajDoner() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", List.of(), 0);

        assertThat(resolver.resolve("rehber_departman", "muhasebe çalışanları").get("departman_bilgisi"))
                .isEqualTo("Muhasebe ve Finans departmanında kayıtlı çalışan görünmüyor.");
    }

    @Test
    void listeUstSiniriAsilirsaKalanSayiBelirtilir() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", List.of(employee("Elif Sahin", "Ofiste")), 30);

        assertThat(resolver.resolve("rehber_departman", "muhasebe çalışanları").get("departman_bilgisi"))
                .contains("30 kişi çalışıyor")
                .contains("ve 29 kişi daha");
    }

    // Ofis durumu girilmemis calisanda ham "null" gorunmemeli.
    @Test
    void durumuOlmayanCalisanNullBasmaz() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", List.of(employee("Test Calisan", null)), 1);

        assertThat(resolver.resolve("rehber_departman", "muhasebe çalışanları").get("departman_bilgisi"))
                .contains("• Test Calisan")
                .doesNotContain("null");
    }

    // A-25 duzeltmesi: durum filtreli sorular ASLINDA calisma_duzeni'ne ait, ama V44
    // ornekleri onlari bu kategoriye cekebiliyor (olculdu: 0.742 / 0.693). Savunma katmani —
    // yanlis kategoriye gelse bile dogru cevap donmeli.
    @Test
    void durumIpucuVarsaListeOnaGoreFiltrelenir() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Uzaktan", List.of(employee("Elif Sahin", "Uzaktan")), 1);

        String reply = resolver.resolve("rehber_departman", "muhasebede kimler uzaktan çalışıyor")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Muhasebe ve Finans departmanında uzaktan çalışan 1 kişi:")
                .contains("• Elif Sahin");
    }

    @Test
    void izindeOlanlarSorusuFiltreliListeDoner() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Izinde", List.of(employee("Burak Yildiz", "Izinde")), 1);

        assertThat(resolver.resolve("rehber_departman", "muhasebe izinde olanlar").get("departman_bilgisi"))
                .contains("izinde olan 1 kişi")
                .contains("• Burak Yildiz");
    }

    // Elle test (2026-08-03): kullanici "ofisde" yazinca durum filtresi hic uygulanmiyor ve
    // TUM liste donuyordu. Durum ipuclari artik kok olarak araniyor.
    @Test
    void durumKelimesininYazimVaryantlariDaTaninir() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Ofiste", List.of(employee("Ayse Kaya", "Ofiste")), 1);

        assertThat(resolver.resolve("rehber_departman", "muhasebede kimler ofisde")
                .get("departman_bilgisi")).contains("ofiste görünen 1 kişi");
    }

    // Iki ipucu birden gecerse sonuc DETERMINISTIK olmali: liste sirasi Map yerine
    // bilerek List ile sabitlendi.
    @Test
    void ikiDurumIpucuVarsaDahaSpesifikOlanSecilir() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Izinde", List.of(employee("Burak Yildiz", "Izinde")), 1);

        assertThat(resolver.resolve("rehber_departman", "muhasebede ofiste olmayıp izinli olanlar")
                .get("departman_bilgisi")).contains("izinde olan 1 kişi");
    }

    // --- A-26 (#173): olumsuz sorgular ---

    // Elle test: "kimler ofiste değil" sorusuna OFISTEKILER listeleniyordu — tam tersi.
    // Artik liste hic uretilmiyor.
    @Test
    void olumsuzSorguListeUretmez() {
        String reply = resolver.resolve("rehber_departman", "muhasebede kimler ofiste değil")
                .get("departman_bilgisi");

        assertThat(reply)
                .contains("Olumsuz sorguları henüz desteklemiyorum")
                .doesNotContain("kişi çalışıyor")
                .doesNotContain("•");
    }

    // "ofiste olmayanlar" LISTE ipucu tasimiyor ("olmayanlar" icinde "olanlar" alt-dizesi
    // yok), dolayisiyla kontrol yalnizca liste dalinda olsaydi bu soru sessizce departman
    // KARTINA duserdi. Kontrol bu yuzden en basta.
    @Test
    void olumsuzlamaninFarkliBicimleriDeYakalanir() {
        assertThat(resolver.resolve("rehber_departman", "muhasebede ofiste olmayanlar")
                .get("departman_bilgisi")).contains("Olumsuz sorguları");
        assertThat(resolver.resolve("rehber_departman", "muhasebede izinliler hariç kimler var")
                .get("departman_bilgisi")).contains("Olumsuz sorguları");
    }

    // Erken cikis: olumsuz sorguda DB'ye hic gidilmiyor.
    @Test
    void olumsuzSorguVeriTabaninaGitmez() {
        resolver.resolve("rehber_departman", "muhasebede ofiste olmayanlar");

        verifyNoInteractions(departmentService, directoryService);
    }

    // NOBETCI: olumsuzlama TASIMAYAN sorular etkilenmemeli.
    @Test
    void olumsuzlamaYoksaListeNormalDoner() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Ofiste", List.of(employee("Ayse Kaya", "Ofiste")), 1);

        assertThat(resolver.resolve("rehber_departman", "muhasebede kimler ofiste")
                .get("departman_bilgisi")).contains("ofiste görünen 1 kişi");
    }

    @Test
    void durumFiltresiSonucsuzsaAcikMesajDoner() {
        seedDepartments();
        whenEmployeeSearch("Muhasebe ve Finans", "Izinde", List.of(), 0);

        assertThat(resolver.resolve("rehber_departman", "muhasebede izinde olanlar").get("departman_bilgisi"))
                .isEqualTo("Muhasebe ve Finans departmanında izinde olan kimse yok.");
    }

    private void whenEmployeeSearch(String department, List<EmployeeResponse> data, long total) {
        whenEmployeeSearch(department, null, data, total);
    }

    // --- A-40 (#209): departman listesinde gun ---

    /**
     * Olculdu (elle test): "muhasebe departmanında çarşamba günü ofiste olanlar kimler"
     * sorusuna BUGUNUN listesi donuyordu. Dolu liste, dogru format, yanlis gun.
     *
     * <p>Stub bilerek {@code wednesday} anahtarina bagli (referans gun Pazartesi).
     */
    @Test
    void sorulanGununListesiDoner() {
        seedDepartments();
        whenEmployeeSearchOnDay("Muhasebe ve Finans", "Ofiste", "wednesday",
                List.of(employee("Ayse Kaya", "Ofiste")), 1);

        String reply = resolver.resolve("rehber_departman",
                        "muhasebe departmanında çarşamba günü ofiste olanlar kimler")
                .get("departman_bilgisi");

        assertThat(reply).contains("Çarşamba").contains("Ayse Kaya");
    }

    // A-37 deseni: tarih ifadesi VAR ama cozulemedi -> bugune dusulmez, sorgu hic atilmaz.
    @Test
    void cozulemeyenTarihBugunuDondurmez() {
        seedDepartments();

        String reply = resolver.resolve("rehber_departman",
                        "muhasebe departmanında ağustos ofiste olanlar")
                .get("departman_bilgisi");

        assertThat(reply).contains("Hangi günü sorduğunu");
        verifyNoInteractions(directoryService);
    }

    private void whenEmployeeSearchOnDay(String department, String status, String day,
                                         List<EmployeeResponse> data, long total) {
        when(directoryService.searchEmployees(isNull(), eq(department), eq(status),
                eq(day), eq(0), anyInt()))
                .thenReturn(new PagedResponse<>(data, 0, 25, total));
    }

    private void whenEmployeeSearch(String department, String status,
                                    List<EmployeeResponse> data, long total) {
        // A-40: gun parametresi any() — gun belirtilmeyen testlerde bugunun anahtari gelir.
        when(directoryService.searchEmployees(isNull(), eq(department),
                status == null ? isNull() : eq(status), any(), eq(0), anyInt()))
                .thenReturn(new PagedResponse<>(data, 0, 25, total));
    }

    private EmployeeResponse employee(String name, String officeStatus) {
        EmployeeResponse response = mock(EmployeeResponse.class);
        lenient().when(response.getName()).thenReturn(name);
        lenient().when(response.getOfficeStatus()).thenReturn(officeStatus);
        return response;
    }

    private void seedDepartments() {
        List<DepartmentResponse> departments = List.of(
                department("Muhasebe ve Finans", "Fatura ve bordro süreçleri",
                        "Elif Sahin", "1005", "elif@company.com"),
                department("Bilgi Teknolojileri", "Sistem ve destek",
                        "Emre Koc", "1002", "emre@company.com"));
        when(departmentService.searchDepartments(isNull(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(departments, 0, 100, 2));
    }

    private DepartmentResponse department(String name, String responsibilities,
                                          String managerName, String managerPhone, String managerEmail) {
        DepartmentResponse response = mock(DepartmentResponse.class);
        lenient().when(response.getName()).thenReturn(name);
        lenient().when(response.getResponsibilities()).thenReturn(responsibilities);
        lenient().when(response.getManagerName()).thenReturn(managerName);
        lenient().when(response.getManagerPhone()).thenReturn(managerPhone);
        lenient().when(response.getManagerEmail()).thenReturn(managerEmail);
        return response;
    }
}
