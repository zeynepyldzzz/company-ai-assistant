package com.company.assistant.survey;

/** C-13 (#121): anket secenegi (calisan ve admin taraflarinda ortak kullanilir). */
public record SurveyOptionDto(Integer id, String optionText) {

    static SurveyOptionDto from(SurveyOption option) {
        return new SurveyOptionDto(option.getId(), option.getOptionText());
    }
}
