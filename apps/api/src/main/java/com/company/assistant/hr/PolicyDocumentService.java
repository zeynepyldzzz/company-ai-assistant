package com.company.assistant.hr;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.common.PagedResponse;

@Service
public class PolicyDocumentService {

    private final PolicyDocumentRepository repository;

    public PolicyDocumentService(PolicyDocumentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DocumentSummary> list(int page, int pageSize) {
        List<DocumentSummary> all = repository.findAllActive();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(pageSize, 1);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PagedResponse<>(all.subList(from, to), page, pageSize, all.size());
    }

    /**
     * POST — yeni dokuman + v1 (is_current=true). procedureId gecersizse 400
     * (FK RESTRICT patlamadan once net hata).
     */
    @Transactional
    public PolicyVersionResponse createDocument(DocumentCreateRequest req, Authentication auth) {
        if (!repository.procedureExists(req.procedureId())) {
            throw new IllegalArgumentException("Gecersiz procedureId: " + req.procedureId());
        }
        int documentId = repository.insertDocument(req.procedureId(), req.title());
        int versionId = repository.insertVersion(documentId, 1, req.content(), req.steps(),
                req.effectiveDate(), true, resolveEmployeeId(auth));
        return repository.findVersionById(versionId).orElseThrow();
    }

    /**
     * PUT — mevcut dokumana yeni versiyon. Once eski current temizlenir, sonra yeni current
     * eklenir (partial unique index sirayi zorunlu kilar); tek transaction (FR-58).
     */
    @Transactional
    public PolicyVersionResponse addVersion(int documentId, VersionCreateRequest req, Authentication auth) {
        requireActive(documentId);
        repository.clearCurrent(documentId);
        int versionNo = repository.nextVersionNo(documentId);
        int versionId = repository.insertVersion(documentId, versionNo, req.content(), req.steps(),
                req.effectiveDate(), true, resolveEmployeeId(auth));
        return repository.findVersionById(versionId).orElseThrow();
    }

    // DELETE — soft delete (deleted_at); versiyon gecmisi korunur (NFR-06).
    @Transactional
    public void delete(int documentId) {
        if (repository.softDelete(documentId) == 0) {
            throw new PolicyDocumentNotFoundException("Dokuman bulunamadi: id=" + documentId);
        }
    }

    @Transactional(readOnly = true)
    public List<PolicyVersionResponse> listVersions(int documentId) {
        requireActive(documentId);
        return repository.findVersions(documentId);
    }

    private void requireActive(int documentId) {
        if (!repository.existsActive(documentId)) {
            throw new PolicyDocumentNotFoundException("Dokuman bulunamadi: id=" + documentId);
        }
    }

    // JWT principal'i employee id'sidir (auth.getName()); denetim izi icin created_by'a yazilir.
    private Integer resolveEmployeeId(Authentication auth) {
        if (auth == null) {
            return null;
        }
        try {
            return Integer.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
