package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Integer> {

    /**
     * A-33 (#192): bu calisanin yanitladigi anketlerin id'leri.
     *
     * <p>Anket basina {@code existsBySurveyIdAndEmployeeId} cagirmak daha okunur olurdu ama
     * aktif anket listesi bir dongu; her satir icin ayri sorgu N+1 demek. Kullanicinin tum
     * yanitlari zaten kucuk bir kume oldugu icin tek sorguda cekip bellekte esliyoruz.
     */
    @Query("SELECT r.survey.id FROM SurveyResponse r WHERE r.employeeId = :employeeId")
    List<Integer> findAnsweredSurveyIds(Integer employeeId);

    /** C-8 (#52): GET /admin/surveys/{id}/results icin bir anketin tum yanitlari. */
    List<SurveyResponse> findAllBySurveyId(Integer surveyId);

    /** C-13 (#121): calisana acik response-count endpoint'i icin (progress bar). */
    long countBySurveyId(Integer surveyId);

    /** C-13 (#121): anket silinirken once bagli yanitlari temizlemek icin (FK constraint). */
    void deleteAllBySurveyId(Integer surveyId);
}
