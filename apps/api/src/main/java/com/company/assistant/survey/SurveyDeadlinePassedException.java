package com.company.assistant.survey;

/** C-13 (#121): anketin deadline'i gecmisse yeni yanit kabul edilmez (409). */
public class SurveyDeadlinePassedException extends RuntimeException {
    public SurveyDeadlinePassedException(String message) {
        super(message);
    }
}
