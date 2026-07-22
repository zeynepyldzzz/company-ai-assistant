package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.ObjectMapper;

/**
 * hr_procedure -> policy_document -> policy_version uc katmanini JdbcTemplate ile okur
 * (JPA yok, mevcut ResponseTemplateRepository deseni).
 * <p>
 * Tum okumalar is_current = true versiyona dayanir (FR-58) ve soft-delete edilmis
 * dokumani haric tutar (policy_document.deleted_at IS NULL).
 */
@Repository
public class HrProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public HrProcedureRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // --- REST: liste (guncel versiyonu olmayan prosedur listede gorunmez) -----------------

    private static final String LIST_SQL = """
            SELECT p.id, p.title, p.category, d.name AS responsible_department,
                   p.responsible_contact, pv.version_no, pv.effective_date
            FROM hr_procedure p
            LEFT JOIN department d ON d.id = p.responsible_department_id
            JOIN policy_document doc ON doc.procedure_id = p.id AND doc.deleted_at IS NULL
            JOIN policy_version pv ON pv.document_id = doc.id AND pv.is_current
            ORDER BY p.id
            """;

    public List<HrProcedureSummary> findAll() {
        return jdbcTemplate.query(LIST_SQL, SUMMARY_MAPPER);
    }

    // --- REST: tekil detay (topic / id) -------------------------------------------------

    private static final String DETAIL_SELECT = """
            SELECT p.id, p.title, p.category, d.name AS responsible_department,
                   p.responsible_contact, pv.version_no, pv.effective_date,
                   pv.content, pv.steps
            FROM hr_procedure p
            LEFT JOIN department d ON d.id = p.responsible_department_id
            JOIN policy_document doc ON doc.procedure_id = p.id AND doc.deleted_at IS NULL
            JOIN policy_version pv ON pv.document_id = doc.id AND pv.is_current
            """;

    public Optional<HrProcedureDetail> findByCategory(String topic) {
        return jdbcTemplate.query(DETAIL_SELECT + " WHERE p.category = ?", detailMapper(), topic)
                .stream().findFirst();
    }

    public Optional<HrProcedureDetail> findById(int id) {
        return jdbcTemplate.query(DETAIL_SELECT + " WHERE p.id = ?", detailMapper(), id)
                .stream().findFirst();
    }

    // --- Chatbot: intent adindan cozumleme (uc durumu ayirt eder) -----------------------

    // LEFT JOIN: hr_procedure satiri her zaman (varsa) doner; current versiyon yoksa
    // pv.* NULL kalir. Bu ayrim resolver'in fallback karari icin gereklidir (§2).
    private static final String BY_INTENT_SQL = """
            SELECT p.title, d.name AS responsible_department, p.responsible_contact,
                   pv.version_no, pv.effective_date, pv.steps
            FROM hr_procedure p
            JOIN intents i ON i.id = p.intent_id
            LEFT JOIN department d ON d.id = p.responsible_department_id
            LEFT JOIN policy_document doc ON doc.procedure_id = p.id AND doc.deleted_at IS NULL
            LEFT JOIN policy_version pv ON pv.document_id = doc.id AND pv.is_current
            WHERE i.name = ?
            """;

    public Optional<HrProcedureData> findByIntentName(String intentName) {
        return jdbcTemplate.query(BY_INTENT_SQL, dataMapper(), intentName)
                .stream().findFirst();
    }

    // --- RowMapper'lar -------------------------------------------------------------------

    private static final RowMapper<HrProcedureSummary> SUMMARY_MAPPER = (rs, rowNum) ->
            new HrProcedureSummary(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("responsible_department"),
                    rs.getString("responsible_contact"),
                    rs.getInt("version_no"),
                    rs.getObject("effective_date", LocalDate.class));

    private RowMapper<HrProcedureDetail> detailMapper() {
        return (rs, rowNum) -> new HrProcedureDetail(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("responsible_department"),
                rs.getString("responsible_contact"),
                rs.getInt("version_no"),
                rs.getObject("effective_date", LocalDate.class),
                rs.getString("content"),
                parseSteps(rs.getString("steps")));
    }

    private RowMapper<HrProcedureData> dataMapper() {
        return (rs, rowNum) -> new HrProcedureData(
                rs.getString("title"),
                rs.getString("responsible_department"),
                rs.getString("responsible_contact"),
                rs.getObject("version_no", Integer.class),
                rs.getObject("effective_date", LocalDate.class),
                parseSteps(rs.getString("steps")));
    }

    // steps JSONB, Postgres tarafindan text formunda doner; Jackson 3 ile diziye cevrilir.
    // CHECK (jsonb_typeof(steps) = 'array') sekli garanti eder; yine de parse hatasi
    // sessizce yutulmaz, veri bozulmasi yuksek sesle bildirilir.
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
