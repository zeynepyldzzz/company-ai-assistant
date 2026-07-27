package com.company.assistant.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.assistant.common.ErrorResponse;

/**
 * C-11 (#85): GET /admin/reports/{type}, GET /admin/reports/{type}/export?format=xlsx|pdf.
 * Sadece system_admin erisebilir (issue kabul kriteri, FR-80-82).
 * MVP: tek rapor tipi "usage", export yalnizca xlsx (pdf kutuphanesi projede
 * henuz yok; kabul kriteri "xlsx veya pdf" oldugu icin xlsx yeterli).
 */
@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class ReportController {

    private static final String USAGE_TYPE = "usage";

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/{type}")
    public UsageReportResponse get(@PathVariable String type) {
        requireUsageType(type);
        return reportService.usageReport();
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type,
                                          @RequestParam(defaultValue = "xlsx") String format) {
        requireUsageType(type);
        if (!"xlsx".equalsIgnoreCase(format)) {
            throw new UnsupportedReportFormatException(
                    "Desteklenmeyen format: " + format + " (yalnizca xlsx destekleniyor)");
        }

        byte[] bytes = reportExportService.toXlsx(reportService.usageReport());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usage-report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    private void requireUsageType(String type) {
        if (!USAGE_TYPE.equalsIgnoreCase(type)) {
            throw new ReportNotFoundException("Bilinmeyen rapor tipi: " + type);
        }
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("REPORT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedReportFormatException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFormat(UnsupportedReportFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("UNSUPPORTED_FORMAT", ex.getMessage()));
    }
}
