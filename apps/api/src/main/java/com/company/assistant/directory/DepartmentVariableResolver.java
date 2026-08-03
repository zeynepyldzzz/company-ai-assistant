package com.company.assistant.directory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.common.TurkishText;

/**
 * A-18: 'rehber_departman' intent'i icin departman iletisim bilgisi.
 *
 * Uretilen anahtar V28 template'iyle eslesir:
 *   {{departman_bilgisi}} -> departman + sorumluluklar + yonetici iletisimi,
 *                            ya da departman anlasilmadiysa mevcut departman listesi
 *
 * Departman adi koda gomulu degil, rehberden turetilir (A-12/A-14 deseni). Departman
 * anlasilmazsa rastgele secim yapilmaz; mevcut departmanlar listelenip kullaniciya sorulur.
 */
@Component
public class DepartmentVariableResolver {

    private static final String DEPARTMENT_INTENT = "rehber_departman";
    private static final String VARIABLE = "departman_bilgisi";

    private static final String UNKNOWN_FIELD = "belirtilmemiş";
    private static final int MAX_DEPARTMENTS = 100;

    /** Tek departman listesi icin ust sinir; OfficeStatusVariableResolver ile ayni deger. */
    private static final int MAX_NAMES = 25;

    /**
     * A-25 (#169): "muhasebe çalışanları" tipi sorular. Ayni intent iki farkli sey isteyebiliyor —
     * departmanin ILETISIM bilgisi ("muhasebe yetkilisi kim") ya da CALISAN LISTESI ("muhasebede
     * kimler var"). A-20'de kisi yanitlarinda kullandigimiz alan tespiti deseni burada da gecerli:
     * sorulan sey belliyse ona gore yanit uretilir.
     *
     * <p>Durum kelimesi ("ofiste", "uzaktan") iceren sorular BURAYA GELMEZ; onlar calisma_duzeni
     * intent'ine ait ve OfficeStatusVariableResolver tarafindan durum filtresiyle yanitlanir.
     * Ayrim kural katmaninda yapiliyor.
     */
    private static final List<String> EMPLOYEE_LIST_CUES =
            List.of("calisan", "kimler", "ekip", "personel", "kisiler", "olanlar");

    /**
     * A-25 (#169) duzeltmesi: durum filtreli sorular ("muhasebede kimler uzaktan çalışıyor")
     * ASLINDA calisma_duzeni'ne ait ve kural katmani onlari buraya yonlendirmiyor. Ama
     * embedding tarafinda ayni koruma yok — olculdu: V44 ornekleri eklendikten sonra
     * "Muhasebede kimler uzaktan çalışıyor" 0.742 ile buraya kaydi.
     *
     * <p>Bu yuzden savunma katmani: yanlis kategoriye gelse bile durum filtresi uygulanir ve
     * kullanici DOGRU cevabi alir. Siniflandirma tarafi ayrica ornek ile duzeltiliyor; ikisi
     * birlikte, cunku tek basina ornek eklemek embedding yarisina bagli ve kirilgan.
     */
    /*
     * Elle test (2026-08-03): kullanici "ofisde" yazdi, kod "ofiste" ariyordu ve durum
     * filtresi hic uygulanmadi — tum liste dondu. Bu yuzden TAM KELIME degil KOK araniyor:
     * "ofis" -> ofiste / ofisde / ofisteki, "izin" -> izinde / izinli / izne.
     *
     * Sirali liste, Map degil: Map.of siralamayi garanti etmez ve "ofiste izinli olanlar"
     * gibi iki ipucu tasiyan cumlede hangisinin secilecegi belirsiz kalirdi. Daha spesifik
     * kokler once; "ofis" en genel oldugu icin en sonda.
     */
    private static final List<Map.Entry<String, String>> STATUS_CUES = List.of(
            Map.entry("izin", "Izinde"),
            Map.entry("uzaktan", "Uzaktan"),
            Map.entry("evden", "Uzaktan"),
            Map.entry("ofis", "Ofiste"));

    /**
     * Departman adlarinda gecen ama ayirt edici olmayan kelimeler. "bilgi" bilerek burada:
     * "Bilgi Teknolojileri" adinin parcasi ama ayni zamanda siradan bir kelime — "bilgisi
     * lazim" gibi cumleler yanlislikla bu departmani secerdi. Eslesme "teknolojileri"
     * uzerinden kurulur.
     */
    private static final List<String> GENERIC_WORDS =
            List.of("departman", "departmani", "birim", "bilgi", "bilgisi", "iletisim");

    private final DepartmentService departmentService;
    private final DirectoryService directoryService;

    public DepartmentVariableResolver(DepartmentService departmentService,
                                      DirectoryService directoryService) {
        this.departmentService = departmentService;
        this.directoryService = directoryService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!DEPARTMENT_INTENT.equals(intentName)) {
            return Map.of();
        }

        List<DepartmentResponse> departments =
                departmentService.searchDepartments(null, 0, MAX_DEPARTMENTS).data();
        if (departments.isEmpty()) {
            return Map.of(VARIABLE, "Sistemde tanımlı departman bulunmuyor.");
        }

        String text = TurkishText.foldToAscii(message);
        boolean wantsRoster = EMPLOYEE_LIST_CUES.stream().anyMatch(text::contains);
        return departments.stream()
                .filter(department -> mentions(text, department.getName()))
                .findFirst()
                .map(department -> Map.of(VARIABLE,
                        wantsRoster ? employeeList(department, text) : departmentCard(department)))
                .orElseGet(() -> Map.of(VARIABLE, askWhichDepartment(departments)));
    }

    /**
     * A-25 (#169): departmanin calisan listesi. Durum filtresi YOK — kullanici "muhasebe
     * çalışanları" dediginde herkesi istiyor. Bu yuzden her satirda ofis durumu gosteriliyor;
     * kullanici filtrelemediginde o bilgi satirda anlamli hale geliyor.
     *
     * <p>Ust sinir OfficeStatusVariableResolver ile ayni gerekceyle var: sirket buyudukce liste
     * okunmaz hale gelir ve alfabetik ilk N kisiyi sessizce kesmek yaniltici olur, o yuzden
     * kalan sayi acikca yazilir.
     */
    private String employeeList(DepartmentResponse department, String foldedText) {
        String status = detectStatus(foldedText);
        PagedResponse<EmployeeResponse> result =
                directoryService.searchEmployees(null, department.getName(), status, 0, MAX_NAMES);
        if (result.data().isEmpty()) {
            return department.getName() + " departmanında " + emptyLabel(status) + ".";
        }

        // Durum filtresi varsa her satirda tekrar yazmak gereksiz — bilgi zaten baslikta.
        String names = result.data().stream()
                .map(employee -> "• " + employee.getName()
                        + (status == null ? statusSuffix(employee) : ""))
                .collect(Collectors.joining("\n"));
        String more = result.total() > result.data().size()
                ? "\n• ve " + (result.total() - result.data().size()) + " kişi daha"
                : "";

        return header(department, status, result.total()) + "\n" + names + more;
    }

    /** Mesajdaki durum ipucu; yoksa null (tum calisanlar listelenir). */
    private String detectStatus(String foldedText) {
        return STATUS_CUES.stream()
                .filter(entry -> foldedText.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String header(DepartmentResponse department, String status, long total) {
        if (status == null) {
            return department.getName() + " departmanında " + total + " kişi çalışıyor:";
        }
        return department.getName() + " departmanında " + statusLabel(status) + " " + total + " kişi:";
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "Uzaktan" -> "uzaktan çalışan";
            case "Izinde" -> "izinde olan";
            default -> "ofiste görünen";
        };
    }

    private String emptyLabel(String status) {
        if (status == null) {
            return "kayıtlı çalışan görünmüyor";
        }
        return statusLabel(status) + " kimse yok";
    }

    private String statusSuffix(EmployeeResponse employee) {
        return employee.getOfficeStatus() != null ? " — " + employee.getOfficeStatus() : "";
    }

    private boolean mentions(String text, String name) {
        if (name == null) {
            return false;
        }
        return List.of(TurkishText.foldToAscii(name).split("[^a-z0-9]+")).stream()
                .filter(word -> word.length() >= 4)
                .filter(word -> !GENERIC_WORDS.contains(word))
                .anyMatch(text::contains);
    }

    // NULL alanlar acik metinle basilir; ham "null" kullaniciya gorunmemeli.
    private String departmentCard(DepartmentResponse department) {
        StringBuilder card = new StringBuilder(department.getName());
        if (department.getResponsibilities() != null && !department.getResponsibilities().isBlank()) {
            card.append("\n").append(department.getResponsibilities());
        }
        card.append("\n• Yönetici: ").append(orUnknown(department.getManagerName()))
                .append("\n• Telefon: ").append(orUnknown(department.getManagerPhone()))
                .append("\n• E-posta: ").append(orUnknown(department.getManagerEmail()));
        return card.toString();
    }

    private String askWhichDepartment(List<DepartmentResponse> departments) {
        return "Hangi departmanı sorduğunu yazarsan iletişim bilgilerini getirebilirim:\n"
                + departments.stream()
                        .map(department -> "• " + department.getName())
                        .collect(Collectors.joining("\n"));
    }

    private String orUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN_FIELD : value;
    }
}
