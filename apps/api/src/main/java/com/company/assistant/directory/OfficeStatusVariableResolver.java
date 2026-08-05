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

    /**
     * A-26 (#173): "ofiste değil" aslinda "uzaktan VEYA izinde" demek — tek bir durum
     * filtresine karsilik gelmiyor. Tam destek ayri bir is; bu mesaj yanlis cevap vermeyi
     * durduruyor ve kullaniciyi calisan bir alternatife yonlendiriyor.
     */
    private static final String NEGATION_UNSUPPORTED =
            "Olumsuz sorguları henüz desteklemiyorum. Bunun yerine \"kimler uzaktan\" "
                    + "ya da \"kimler izinde\" diye sorabilirsin.";

    /**
     * A-32 (#188): etiketler artik {@link OfficeStatusLabels}'dan geliyor. Onceden burada
     * kopyalari duruyordu; ayni degerin iki yerde yasamasi, biri degisip digeri unutuldugunda
     * filtrenin sessizce hicbir seyle eslesmemesi demekti.
     */
    private static final String STATUS_OFFICE = OfficeStatusLabels.OFFICE;
    private static final String STATUS_REMOTE = OfficeStatusLabels.REMOTE;
    private static final String STATUS_LEAVE = OfficeStatusLabels.LEAVE;

    /**
     * Sirket geneli kapsam ipuclari. "genel" BILEREK yok: departman adlarindan biri
     * "Genel Mudurluk" olursa "sirket geneli" sorusu o departmana kayardi. "sirket"
     * ifadenin kendisinde zaten bulunuyor.
     */
    private static final Pattern COMPANY_WIDE = Pattern.compile("\\b(sirket|tum|butun|herkes)");

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

    /**
     * Ucuncu sahis sorusu mu ("kimler ofiste") — kullanicinin kendi plani mi ("ofiste miyim").
     *
     * A-20 (#139): desen TurkishText'e tasindi, cunku kural katmani da ayni ayrimi yapmak
     * zorunda ("kimler ofiste" tek kisilik rehber yanitina kaymamali). Iki katman ayni
     * deseni okumazsa aradaki bosluktan yanlis intent sizar.
     */
    public boolean isThirdPersonQuestion(String foldedText) {
        return TurkishText.mentionsThirdPersonGroup(foldedText);
    }

    /**
     * @param foldedText TurkishText.foldToAscii'den gecmis kullanici mesaji
     * @param employeeId JWT'den gelen kimlik (FR-63)
     */
    public String resolve(String foldedText, Integer employeeId) {
        // A-26 (#173): olumsuz sorguda liste URETILMEZ. resolveStatus() "degil" kelimesini
        // gormezden gelir ve "kimler ofiste değil" sorusuna OFISTEKILERI listelerdi — tam
        // tersi. Kontrol en basta, cunku hangi kapsamda (departman / sirket geneli)
        // sorulduguna bakilmaksizin yanit yanlis olurdu.
        if (TurkishText.mentionsNegation(foldedText)) {
            return NEGATION_UNSUPPORTED;
        }

        String status = resolveStatus(foldedText);
        String department = departmentFromMessage(foldedText);

        // A-20 (#139): "sirkette kimler ofiste" sorusu departman kapsamina hapsolmamali.
        // Departman adi ACIKCA gectiyse o kazanir — daha dar kapsam her zaman daha
        // spesifik cevaptir ("tum satis ekibinden kimler ofiste" -> Satis).
        if (department == null && COMPANY_WIDE.matcher(foldedText).find()) {
            return companyWideList(status);
        }

        long companyTotal = countByStatus(status);
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

    /**
     * Sirket geneli liste. Departman kapsamli daldan ayri bir metot: orada departman adi
     * baslikta bir kez yazilir, burada her satirda kisiye eslik etmesi gerekiyor — yoksa
     * "40 isim" duz bir yigin olur. Ayrica bu dalda ayri bir sayim sorgusu atilmaz;
     * result.total() zaten sirket toplamidir.
     */
    private String companyWideList(String status) {
        PagedResponse<EmployeeResponse> result =
                directoryService.searchEmployees(null, null, status, 0, MAX_NAMES);
        if (result.data().isEmpty()) {
            return "Şirket genelinde " + emptyLabel(status) + ".";
        }

        String names = result.data().stream()
                .map(e -> "• " + e.getName() + departmentSuffix(e))
                .collect(Collectors.joining("\n"));
        String more = result.total() > result.data().size()
                ? "\n• ve " + (result.total() - result.data().size()) + " kişi daha"
                : "";

        return "Şirket genelinde " + listLabel(status) + " (" + result.total() + " kişi):\n"
                + names + more;
    }

    private String departmentSuffix(EmployeeResponse employee) {
        return employee.getDepartmentName() != null ? " — " + employee.getDepartmentName() : "";
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
