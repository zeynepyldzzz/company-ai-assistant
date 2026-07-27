package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * A-T2 / AC2: yeni doküman versiyonu yuklendiginde eski versiyon SILINMEZ, sadece
 * is_current=false olur (FR-58 denetim izi, NFR-06). Servis+repository+DB uctan uca.
 * <p>
 * Gercek DB'ye yazar; @Transactional ile her test sonunda rollback (seed kirlenmez).
 * Mock testi (PolicyDocumentServiceTest) sirayi dogrular; bu test satirin gercekten
 * DB'de kaldigini dogrular.
 */
@SpringBootTest
@Transactional
class PolicyDocumentVersioningIntegrationTest {

    @Autowired
    private PolicyDocumentService service;

    @Autowired
    private HrProcedureService procedureService;

    @Autowired
    private PolicyDocumentRepository repository;

    @Test
    void yeniVersiyon_eskisiSilinmezSadeceCurrentDuser() {
        // Gecerli bir procedureId (seed'den "izin" prosedurunun gercek id'si).
        int procedureId = procedureService.getByTopic("izin").id();

        var v1 = service.createDocument(new DocumentCreateRequest(
                procedureId, "Versiyon IT Dokumani", "v1 icerik", List.of(),
                LocalDate.of(2026, 1, 1)), null);
        int documentId = v1.documentId();

        service.addVersion(documentId, new VersionCreateRequest(
                "v2 icerik", List.of(), LocalDate.of(2026, 2, 1)), null);

        List<PolicyVersionResponse> versions = repository.findVersions(documentId);

        // Eski versiyon HALA DB'de -> iki satir (silinmedi).
        assertThat(versions).hasSize(2);

        var eski = versions.stream().filter(v -> v.versionNo() == 1).findFirst().orElseThrow();
        var yeni = versions.stream().filter(v -> v.versionNo() == 2).findFirst().orElseThrow();

        assertThat(eski.isCurrent()).as("eski versiyon artik current olmamali").isFalse();
        assertThat(eski.content()).as("eski versiyonun icerigi korunmali").isEqualTo("v1 icerik");
        assertThat(yeni.isCurrent()).as("yeni versiyon current olmali").isTrue();
    }
}
