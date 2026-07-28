package com.company.assistant.directory;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.common.TurkishText;

/**
 * A-14 (#115): "kimler ofiste / kimler uzaktan" sorulari icin rehber kaynakli yanit uretir.
 *
 * Veri kaynagi employee.office_status — GET /employees ile employee rolune ZATEN acik ve ofis
 * durumuna gore filtrelenebilir durumda (rehber ekrani). Haftalik plan (weekly_schedule) toplu
 * gorunumu bu sinifta KULLANILMAZ; o /admin/schedules altinda admin bilgisi olarak kalir.
 *
 * Liste her zaman tek bir departmanla sinirlidir: sirket buyudukce "kimler ofiste" 80 satirlik
 * bir liste uretir ve alfabetik ilk N kisiyi kesmek keyfi olur. Departman kapsami + sirket
 * geneli sayi, hem anlamli hem sinirli bir yanit verir.
 *
 * Sinif directory paketinde: sorgular rehber verisine ait. ScheduleVariableResolver ucuncu
 * sahis ipucu gordugunde buraya delege eder, boylece {{calisma_duzenim}} degiskeninin tek bir
 * sahibi olur (iki resolver ayni anahtari doldurursa merge sirasi kirilgan hale gelir).
 */
@Component
public class OfficeStatusVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(OfficeStatusVariableResolver.class);

    /** office_status DB'de serbest metin; degerler packages/shared OfficeStatusSchema ile sabit. */
    private static final String STATUS_OFFICE = "Ofiste";
    private static final String STATUS_REMOTE = "Uzaktan";
    private static final String STATUS_LEAVE = "Izinde";

    // "kim" / "kimler" / "kimlerin" — kelime siniriyla, aksi halde "kimlik", "kimse" gibi
    // kelimeler eslesir. Grup parantezi sart: "kimler?" ifadesi "kimle" + opsiyonel "r"
    // anlamina gelir ve tek basina "kim"i kacirir.
    private static final Pattern THIRD_PERSON = Pattern.compile("\\bkim(ler(in)?)?\\b");

    // Tek departman listesi icin ust sinir; asilirsa "ve N kisi daha" ile kesilir.
    private static final int MAX_NAMES = 25;
    private static final int MAX_DEPARTMENTS = 100;

    private final DirectoryService directoryService;
    private final DepartmentService departmentService;

    public OfficeStatusVariableResolver(DirectoryService directoryService,
                                        DepartmentService departmentService) {
        this.directoryService = directoryService;
        this.departmentService = departmentService;
    }

    /** Ucuncu sahis sorusu mu ("kimler ofiste") — kullanicinin kendi plani mi ("ofiste miyim"). */
    public boolean isThirdPersonQuestion(String foldedText) {
        return THIRD_PERSON.matcher(foldedText).find();
    }

    /**
     * @param foldedText TurkishText.foldToAscii'den gecmis kullanici mesaji
     * @param employeeId JWT'den gelen kimlik (FR-63)
     */
    public String resolve(String foldedText, Integer employeeId) {
        String status = resolveStatus(foldedText);
        long companyTotal = countByStatus(status);

        String department = departmentFromMessage(foldedText);
        if (department == null) {
            department = ownDepartment(employeeId);
        }
        if (department == null) {
            // Departmani olmayan kayitlar var; rastgele bir departman secmek yerine soruyoruz.
            return "Hangi departmanı sorduğunu yazarsan listeleyebilirim. "
                    + companySentence(status, companyTotal);
        }

        PagedResponse<EmployeeResponse> result =
                directoryService.searchEmployees(null, department, status, 0, MAX_NAMES);
        if (result.data().isEmpty()) {
            return department + " departmanında " + emptyLabel(status) + ". "
                    + companySentence(status, companyTotal);
        }

        String names = result.data().stream()
                .map(e -> "• " + e.getName())
                .collect(Collectors.joining("\n"));
        String more = result.total() > result.data().size()
                ? "\n• ve " + (result.total() - result.data().size()) + " kişi daha"
                : "";

        return department + " departmanında " + listLabel(status) + ":\n"
                + names + more + "\n\n" + companySentence(status, companyTotal);
    }

    private String resolveStatus(String text) {
        if (text.contains("uzaktan") || text.contains("remote") || text.contains("evden")) {
            return STATUS_REMOTE;
        }
        if (text.contains("izin")) {
            return STATUS_LEAVE;
        }
        return STATUS_OFFICE;
    }

    /** Departman adlari koda gomulu degil; rehberdeki adlardan turetilir (A-12 hat eslestirme deseni). */
    private String departmentFromMessage(String text) {
        List<DepartmentResponse> departments =
                departmentService.searchDepartments(null, 0, MAX_DEPARTMENTS).data();
        for (DepartmentResponse department : departments) {
            boolean mentioned = List.of(TurkishText.foldToAscii(department.getName()).split("[^a-z0-9]+"))
                    .stream()
                    .filter(word -> word.length() >= 4)
                    .anyMatch(text::contains);
            if (mentioned) {
                return department.getName();
            }
        }
        return null;
    }

    private String ownDepartment(Integer employeeId) {
        try {
            return directoryService.getEmployeeById(employeeId).getDepartmentName();
        } catch (EmployeeNotFoundException e) {
            log.warn("Chatbot kullanicisi rehberde bulunamadi: id={}", employeeId);
            return null;
        }
    }

    // office_status'u NULL olan calisanlar hicbir gruba dahil edilmez: sorgu tam esitlik
    // kullandigi icin NULL kayitlar zaten disarida kalir.
    private long countByStatus(String status) {
        return directoryService.searchEmployees(null, null, status, 0, 1).total();
    }

    private String companySentence(String status, long total) {
        return "Şirket genelinde " + total + " kişi " + shortLabel(status) + " görünüyor.";
    }

    // Etiketler bilerek uc ayri bicimde: tek bir metin uc cumle kalibinda da dogru durmuyor
    // ("uzaktan çalışıyor görünenler" bozuk).
    private String listLabel(String status) {
        return switch (status) {
            case STATUS_REMOTE -> "uzaktan çalışanlar";
            case STATUS_LEAVE -> "izinde olanlar";
            default -> "ofiste görünenler";
        };
    }

    private String emptyLabel(String status) {
        return switch (status) {
            case STATUS_REMOTE -> "uzaktan çalışan kimse yok";
            case STATUS_LEAVE -> "izinde kimse yok";
            default -> "ofiste görünen kimse yok";
        };
    }

    private String shortLabel(String status) {
        return switch (status) {
            case STATUS_REMOTE -> "uzaktan";
            case STATUS_LEAVE -> "izinde";
            default -> "ofiste";
        };
    }
}
