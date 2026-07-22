package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.ObjectMapper;

/**
 * A-6: policy_document / policy_version admin CRUD (JdbcTemplate, JPA yok).
 * Yazma islemleri icerir; chatbot'un salt-okunur HrProcedureRepository'sinden ayridir.
 * <p>
 * Tek is_current kurali DB'de {@code ux_policy_version_current} kismi unique index ile
 * zorlanir (V14); bu repository sirayi (once temizle, sonra ekle) korur.
 */
@Repository
public class PolicyDocumentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PolicyDocumentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // --- Varlik kontrolleri --------------------------------------------------------------

    public boolean procedureExists(int procedureId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM hr_procedure WHERE id = ?)",
                Boolean.class, procedureId));
    }

    public boolean existsActive(int documentId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM policy_document WHERE id = ? AND deleted_at IS NULL)",
                Boolean.class, documentId));
    }

    // --- Yazma ---------------------------------------------------------------------------

    public int insertDocument(int procedureId, String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO policy_document (procedure_id, title)
                VALUES (?, ?)
                RETURNING id
                """, Integer.class, procedureId, title);
    }

    public int nextVersionNo(int documentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) + 1 FROM policy_version WHERE document_id = ?",
                Integer.class, documentId);
    }

    // Yeni current versiyon eklenmeden once cagrilir; partial unique index sirayi zorunlu kilar.
    public void clearCurrent(int documentId) {
        jdbcTemplate.update(
                "UPDATE policy_version SET is_current = false WHERE document_id = ? AND is_current",
                documentId);
    }

    public int insertVersion(int documentId, int versionNo, String content,
                             List<ProcedureStep> steps, LocalDate effectiveDate,
                             boolean isCurrent, Integer createdBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO policy_version
                    (document_id, version_no, content, steps, effective_date, is_current, created_by)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, ?)
                RETURNING id
                """, Integer.class,
                documentId, versionNo, content, writeSteps(steps),
                effectiveDate, isCurrent, createdBy);
    }

    // Soft delete: etkilenen satir sayisi doner (0 -> yok veya zaten silik).
    public int softDelete(int documentId) {
        return jdbcTemplate.update(
                "UPDATE policy_document SET deleted_at = now() WHERE id = ? AND deleted_at IS NULL",
                documentId);
    }

    // --- Okuma ---------------------------------------------------------------------------

    private static final String LIST_SQL = """
            SELECT d.id, d.procedure_id, d.title, p.category AS procedure_category,
                   pv.version_no AS current_version_no, pv.effective_date AS current_effective_date,
                   d.created_at
            FROM policy_document d
            JOIN hr_procedure p ON p.id = d.procedure_id
            LEFT JOIN policy_version pv ON pv.document_id = d.id AND pv.is_current
            WHERE d.deleted_at IS NULL
            ORDER BY d.id
            """;

    public List<DocumentSummary> findAllActive() {
        return jdbcTemplate.query(LIST_SQL, SUMMARY_MAPPER);
    }

    private static final String VERSION_SELECT = """
            SELECT id, document_id, version_no, content, steps, effective_date,
                   is_current, created_at, created_by
            FROM policy_version
            """;

    public List<PolicyVersionResponse> findVersions(int documentId) {
        return jdbcTemplate.query(
                VERSION_SELECT + " WHERE document_id = ? ORDER BY version_no",
                versionMapper(), documentId);
    }

    public Optional<PolicyVersionResponse> findVersionById(int versionId) {
        return jdbcTemplate.query(VERSION_SELECT + " WHERE id = ?", versionMapper(), versionId)
                .stream().findFirst();
    }

    // --- RowMapper'lar / JSON ------------------------------------------------------------

    private static final RowMapper<DocumentSummary> SUMMARY_MAPPER = (rs, rowNum) ->
            new DocumentSummary(
                    rs.getInt("id"),
                    rs.getInt("procedure_id"),
                    rs.getString("title"),
                    rs.getString("procedure_category"),
                    rs.getObject("current_version_no", Integer.class),
                    rs.getObject("current_effective_date", LocalDate.class),
                    rs.getTimestamp("created_at").toInstant());

    private RowMapper<PolicyVersionResponse> versionMapper() {
        return (rs, rowNum) -> new PolicyVersionResponse(
                rs.getInt("id"),
                rs.getInt("document_id"),
                rs.getInt("version_no"),
                rs.getString("content"),
                parseSteps(rs.getString("steps")),
                rs.getObject("effective_date", LocalDate.class),
                rs.getBoolean("is_current"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("created_by", Integer.class));
    }

    private String writeSteps(List<ProcedureStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            throw new IllegalStateException("steps JSON'a cevrilemedi", e);
        }
    }

    private List<ProcedureStep> parseSteps(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, ProcedureStep[].class));
        } catch (Exception e) {
            throw new IllegalStateException("policy_version.steps parse edilemedi: " + json, e);
        }
    }
}
