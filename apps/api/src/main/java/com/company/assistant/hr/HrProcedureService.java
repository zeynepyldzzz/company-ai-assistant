package com.company.assistant.hr;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.common.PagedResponse;

@Service
public class HrProcedureService {

    private final HrProcedureRepository repository;

    public HrProcedureService(HrProcedureRepository repository) {
        this.repository = repository;
    }

    /**
     * GET /hr/procedures — 4 prosedurluk kucuk sabit kume; sayfalama bellekte dilimlenir
     * (JdbcTemplate LIMIT/OFFSET bu boyut icin gereksiz). Yanit {data,page,pageSize,total}
     * zarfinda doner (apiEndpoints §0).
     */
    @Transactional(readOnly = true)
    public PagedResponse<HrProcedureSummary> list(int page, int pageSize) {
        List<HrProcedureSummary> all = repository.findAll();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(pageSize, 1);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PagedResponse<>(all.subList(from, to), page, pageSize, all.size());
    }

    // GET /hr/procedures?topic= — tekil nesne; gecersiz topic 404 (dokuman §3).
    @Transactional(readOnly = true)
    public HrProcedureDetail getByTopic(String topic) {
        return repository.findByCategory(topic)
                .orElseThrow(() -> new HrProcedureNotFoundException(
                        "Prosedur bulunamadi (topic=" + topic + ")"));
    }

    // GET /hr/procedures/{id}
    @Transactional(readOnly = true)
    public HrProcedureDetail getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new HrProcedureNotFoundException(
                        "Prosedur bulunamadi (id=" + id + ")"));
    }
}
