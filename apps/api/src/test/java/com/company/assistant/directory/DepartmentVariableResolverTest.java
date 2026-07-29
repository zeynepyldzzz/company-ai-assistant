package com.company.assistant.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
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
 * A-18 (#127): departman iletisim bilgisi. Odak: departman eslestirme, anlasilmayan
 * departmanda netlestirme ve NULL alanlar.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentVariableResolverTest {

    @Mock
    private DepartmentService departmentService;

    private DepartmentVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DepartmentVariableResolver(departmentService);
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
