package com.company.assistant.directory;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;

/**
 * A-39 (#212): mevcut SAYIMI soran sorular — "toplam kaç çalışan var", "kaç departman var".
 *
 * <p><b>Neden ayri bir intent.</b> Bu sorularin karsiligi yoktu ve sessizce baska bir soruya
 * cevap veriliyordu. A-38 hatanin bicimini degistirdi ama kaldirmadi: {@code kac calisan}
 * kalibi ucuncu sahis desenine eklenince "toplam kaç çalışan var" sorusu kullanicinin kendi
 * planindan OFISTEKILERIN LISTESINE dondu. Ikisi de yanlis; yenisi dolu ve formatli oldugu
 * icin fark edilmesi daha zor.
 *
 * <p><b>Durum sayimi bu sinifa ait DEGIL.</b> "kaç kişi ofiste" bir DURUM sorusudur ve
 * {@code calisma_duzeni} -> {@link OfficeStatusVariableResolver} tarafindan yanitlanir; orada
 * liste ve sirket toplami birlikte donuyor ve bu davranis bozulmamali. Buradaki soru mevcut
 * kadro: durum filtresi yok.
 *
 * <p>Ayrim siniflandirmada yapiliyor (ornek cumleler + IntentCalibrationIT). Kural katmanina
 * dokunulmadi: {@code ScheduleVariableResolver} yalnizca {@code calisma_duzeni} intent'inde
 * calisiyor, dolayisiyla soru bu intent'e giderse o dal hic acilmiyor.
 */
@Component
public class CountVariableResolver {

    private static final String COUNT_INTENT = "sayim";
    private static final String VARIABLE = "sayim_bilgisi";

    private static final List<String> DEPARTMENT_CUES = List.of("departman", "bolum", "birim");
    private static final List<String> EMPLOYEE_CUES =
            List.of("calisan", "kisi", "personel", "kadro", "eleman");

    private final DirectoryService directoryService;
    private final DepartmentService departmentService;

    public CountVariableResolver(DirectoryService directoryService,
                                 DepartmentService departmentService) {
        this.directoryService = directoryService;
        this.departmentService = departmentService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!COUNT_INTENT.equals(intentName)) {
            return Map.of();
        }

        String text = TurkishText.foldToAscii(message);
        boolean asksDepartment = containsAny(text, DEPARTMENT_CUES);
        boolean asksEmployee = containsAny(text, EMPLOYEE_CUES);

        // Ipucu yoksa IKISI birden doner. Belirsiz bir sayim sorusunda ("kaç tane var")
        // rastgele birini secmek yerine iki sayiyi da vermek hem dogru hem kisa; kullanici
        // hangisini istediyse onu okur.
        if (asksDepartment && !asksEmployee) {
            return answer("Şirkette " + departmentCount() + " departman var.");
        }
        if (asksEmployee && !asksDepartment) {
            return answer("Şirkette " + employeeCount() + " aktif çalışan var.");
        }
        return answer("Şirkette " + employeeCount() + " aktif çalışan ve "
                + departmentCount() + " departman var.");
    }

    /**
     * Sayfa boyutu 1: yalnizca {@code total} okunuyor, satirlar cekilmiyor.
     * {@link OfficeStatusVariableResolver#countByStatus} ile ayni desen.
     *
     * <p>Aktiflik filtresi sorgunun icinde ({@code e.active = true}); ayrica bir sey yapmaya
     * gerek yok. {@code day} parametresi verilmiyor — durum filtresi olmadigi icin gunun bu
     * sayiya etkisi yok.
     */
    private long employeeCount() {
        return directoryService.searchEmployees(null, null, null, 0, 1).total();
    }

    /**
     * {@code searchDepartments()} DEGIL: o metot yonetici bilgisini de kurdugu icin lazy proxy
     * aciyor ve HTTP istegi disinda LazyInitializationException veriyor. Kural katmani da ayni
     * sebeple ad projeksiyonunu kullaniyor; sayim icin ad listesi zaten yeterli.
     */
    private long departmentCount() {
        return departmentService.getDepartmentNames().size();
    }

    private Map<String, String> answer(String body) {
        return Map.of(VARIABLE, body);
    }

    private boolean containsAny(String text, List<String> cues) {
        return cues.stream().anyMatch(text::contains);
    }
}
