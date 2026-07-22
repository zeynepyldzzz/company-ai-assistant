package com.company.assistant.hr;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.common.PagedResponse;

import jakarta.validation.Valid;

/**
 * A-6: İK dokuman/politika versiyonu admin CRUD (FR-11-13, 77-78; NFR-06).
 * <p>
 * /admin/** SecurityConfig'te ROLE_ADMIN ister; method guard bunu daraltir: sadece
 * hr_admin / system_admin (issue kabul kriteri). AdminController ayni guard desenini kullanir.
 */
@RestController
@RequestMapping("/admin/knowledge-base/documents")
@PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class PolicyDocumentController {

    private final PolicyDocumentService service;

    public PolicyDocumentController(PolicyDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<DocumentSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(page, pageSize);
    }

    @PostMapping
    public ResponseEntity<PolicyVersionResponse> create(
            @Valid @RequestBody DocumentCreateRequest request,
            Authentication authentication) {
        PolicyVersionResponse created = service.createDocument(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public PolicyVersionResponse addVersion(
            @PathVariable int id,
            @Valid @RequestBody VersionCreateRequest request,
            Authentication authentication) {
        return service.addVersion(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Versiyon gecmisi kronolojik doner; apiEndpoints §0 geregi liste zarfi.
    @GetMapping("/{id}/versions")
    public PagedResponse<PolicyVersionResponse> versions(@PathVariable int id) {
        List<PolicyVersionResponse> versions = service.listVersions(id);
        return new PagedResponse<>(versions, 0, versions.size(), versions.size());
    }

    @ExceptionHandler(PolicyDocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PolicyDocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("POLICY_DOCUMENT_NOT_FOUND", ex.getMessage()));
    }
}
