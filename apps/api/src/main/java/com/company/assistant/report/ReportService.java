package com.company.assistant.report;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.company.assistant.chatbot.ChatMessageLogRepository;
import com.company.assistant.survey.SurveyResponseRepository;
import com.company.assistant.vehicle.ReservationRepository;

/**
 * C-11 (#85): MVP kapsaminda tek rapor tipi ("usage") - modul kullanim
 * ozeti. Sistemde tum modulleri kapsayan ortak bir aktivite/audit tablosu
 * olmadigi icin her modulun kendi tablosundan sayim yapiliyor.
 */
@Service
public class ReportService {

    private final ChatMessageLogRepository chatMessageLogRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(ChatMessageLogRepository chatMessageLogRepository,
                          SurveyResponseRepository surveyResponseRepository,
                          ReservationRepository reservationRepository) {
        this.chatMessageLogRepository = chatMessageLogRepository;
        this.surveyResponseRepository = surveyResponseRepository;
        this.reservationRepository = reservationRepository;
    }

    public UsageReportResponse usageReport() {
        List<UsageReportRow> rows = List.of(
                new UsageReportRow("Chatbot", "Toplam Soru", chatMessageLogRepository.countTotal()),
                new UsageReportRow("Chatbot", "Eslesen Soru", chatMessageLogRepository.countMatched()),
                new UsageReportRow("Anketler", "Toplam Yanit", surveyResponseRepository.count()),
                new UsageReportRow("Arac Rezervasyonu", "Toplam Rezervasyon", reservationRepository.count())
        );
        return new UsageReportResponse("usage", Instant.now(), rows);
    }
}
