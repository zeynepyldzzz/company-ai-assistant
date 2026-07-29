package com.company.assistant.directory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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

    /**
     * Departman adlarinda gecen ama ayirt edici olmayan kelimeler. "bilgi" bilerek burada:
     * "Bilgi Teknolojileri" adinin parcasi ama ayni zamanda siradan bir kelime — "bilgisi
     * lazim" gibi cumleler yanlislikla bu departmani secerdi. Eslesme "teknolojileri"
     * uzerinden kurulur.
     */
    private static final List<String> GENERIC_WORDS =
            List.of("departman", "departmani", "birim", "bilgi", "bilgisi", "iletisim");

    private final DepartmentService departmentService;

    public DepartmentVariableResolver(DepartmentService departmentService) {
        this.departmentService = departmentService;
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
        return departments.stream()
                .filter(department -> mentions(text, department.getName()))
                .findFirst()
                .map(department -> Map.of(VARIABLE, departmentCard(department)))
                .orElseGet(() -> Map.of(VARIABLE, askWhichDepartment(departments)));
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
