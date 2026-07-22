package com.company.assistant.hr;

// policy_version.steps JSONB dizisinin tek elemani (V15 seed sekli):
// { "order": 1, "title": "...", "detail": "..." }
// Jackson 3, record bilesen adlarina gore deserialize eder (-parameters acik).
public record ProcedureStep(int order, String title, String detail) {
}
