package com.company.assistant.survey;

/** C-8 (#52): taslak (published=false) bir ankete yanit gonderilmeye calisildiginda. */
public class SurveyNotPublishedException extends RuntimeException {
    public SurveyNotPublishedException(String message) {
        super(message);
    }
}
