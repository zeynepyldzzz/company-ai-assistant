package com.company.assistant.survey;

/** C-13 (#121): ayni calisan ayni ankete ikinci kez oy vermeye calisirsa (409). */
public class SurveyAlreadyRespondedException extends RuntimeException {
    public SurveyAlreadyRespondedException(String message) {
        super(message);
    }
}
