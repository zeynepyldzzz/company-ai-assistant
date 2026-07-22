package com.company.assistant.hr;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İK prosedur intent'leri icin template degiskenlerini uretir (dokuman §2).
 * ChatVariableResolver deseni; farki: kullaniciya degil, intent <b>adina</b> gore cozer.
 * Uretilen anahtarlar V15 template'leriyle birebir eslesir:
 * prosedur_basligi, prosedur_adimlari, sorumlu_departman, sorumlu_iletisim,
 * gecerlilik_tarihi, versiyon.
 */
@Component
public class HrProcedureVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(HrProcedureVariableResolver.class);

    private final HrProcedureRepository repository;

    public HrProcedureVariableResolver(HrProcedureRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public HrProcedureResolution resolve(String intentName) {
        Optional<HrProcedureData> found = repository.findByIntentName(intentName);
        if (found.isEmpty()) {
            return HrProcedureResolution.notApplicable();
        }

        HrProcedureData data = found.get();
        if (!data.hasCurrentVersion()) {
            log.warn("İK intent '{}' icin guncel policy_version yok — fallback template'ine dusulecek.",
                    intentName);
            return HrProcedureResolution.fallback();
        }

        // TemplateRenderer bilinmeyen placeholder'i oldugu gibi birakir; sizinti olmamasi
        // icin her anahtar mutlaka doldurulur (null alanlar bos string).
        Map<String, String> variables = new HashMap<>();
        variables.put("prosedur_basligi", nullToEmpty(data.title()));
        variables.put("prosedur_adimlari", formatSteps(data.steps()));
        variables.put("sorumlu_departman", nullToEmpty(data.responsibleDepartment()));
        variables.put("sorumlu_iletisim", nullToEmpty(data.responsibleContact()));
        variables.put("gecerlilik_tarihi",
                data.effectiveDate() != null ? data.effectiveDate().toString() : "");
        variables.put("versiyon",
                data.versionNo() != null ? data.versionNo().toString() : "");
        return HrProcedureResolution.of(variables);
    }

    // steps'i "1. Baslik: Detay" bicinde numarali tek metne duzlestirir (renderer liste acmaz).
    private String formatSteps(List<ProcedureStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        return steps.stream()
                .sorted(Comparator.comparingInt(ProcedureStep::order))
                .map(s -> s.order() + ". " + s.title() + ": " + s.detail())
                .collect(Collectors.joining("\n"));
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
