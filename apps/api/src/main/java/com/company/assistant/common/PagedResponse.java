package com.company.assistant.common;

import java.util.List;

// docs/apiEndpoints.md #0 Genel Kurallar: liste donen endpoint'ler bu zarfi kullanir.
public record PagedResponse<T>(List<T> data, int page, int pageSize, long total) {
}
