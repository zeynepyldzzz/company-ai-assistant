package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.company.assistant.hr.HrProcedureResolution;
import com.company.assistant.hr.HrProcedureVariableResolver;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private IntentClassificationService classificationService;
    @Mock
    private TemplateResponseService templateResponseService;
    @Mock
    private ChatVariableResolver variableResolver;
    @Mock
    private HrProcedureVariableResolver hrProcedureVariableResolver;
    @Mock
    private ChatMessageLogRepository logRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void eslesenSoruLoglanir() {
        var result = new IntentClassificationService.IntentResult(
                "yemek_menusu", 0.91, "bugün yemekte ne var", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Bugün mercimek çorbası var.");

        chatMessageService.handleMessage("bugün yemekte ne var?", null);

        var captor = ArgumentCaptor.forClass(ChatMessageLogEntry.class);
        verify(logRepository).insert(captor.capture());

        assertThat(captor.getValue()).satisfies(entry -> {
            assertThat(entry.question()).isEqualTo("bugün yemekte ne var?");
            assertThat(entry.intent()).isEqualTo("yemek_menusu");
            assertThat(entry.similarity()).isEqualTo(0.91);
            assertThat(entry.matchedPhrase()).isEqualTo("bugün yemekte ne var");
            assertThat(entry.matched()).isTrue();
            assertThat(entry.threshold()).isEqualTo(0.68);
            assertThat(entry.responseTimeMs()).isNotNull();
        });
    }

    @Test
    void fallbackSorusuDaLoglanir() {
        var result = new IntentClassificationService.IntentResult(
                "intent_bulunamadi", 0.41, "iyi günler", false);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Bu soruyu anlayamadım.");

        chatMessageService.handleMessage("kedim neden miyavlıyor", null);

        var captor = ArgumentCaptor.forClass(ChatMessageLogEntry.class);
        verify(logRepository).insert(captor.capture());

        assertThat(captor.getValue()).satisfies(entry -> {
            assertThat(entry.question()).isEqualTo("kedim neden miyavlıyor");
            assertThat(entry.intent()).isEqualTo("intent_bulunamadi");
            assertThat(entry.matched()).isFalse();
            assertThat(entry.matchedPhrase()).isEqualTo("iyi günler");
            assertThat(entry.similarity()).isEqualTo(0.41);
        });
    }

    @Test
    void logYazilamazsaKullaniciYanitiDusmez() {
        var result = new IntentClassificationService.IntentResult(
                "servis_saati", 0.88, "servis kaçta kalkıyor", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Servis 18:00'de kalkıyor.");
        doThrow(new DataAccessResourceFailureException("DB yok"))
                .when(logRepository).insert(any());

        var response = chatMessageService.handleMessage("servis kaçta kalkıyor?", null);

        assertThat(response).isNotNull();
        assertThat(response.reply()).isEqualTo("Servis 18:00'de kalkıyor.");
    }

    // A-5 / FR-54: İK intent'i icin İK degiskenleri kullanici degiskenleriyle merge edilip
    // buildResponse'a intent'in kendi adiyla gecirilir.
    @Test
    @SuppressWarnings("unchecked")
    void ikIntentiIcinDegiskenlerMergeEdilir() {
        var result = new IntentClassificationService.IntentResult(
                "izin_prosedur", 0.93, "yıllık izin nasıl alınır", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of("departman", "Bilgi Teknolojileri"));
        when(hrProcedureVariableResolver.resolve("izin_prosedur"))
                .thenReturn(HrProcedureResolution.of(Map.of(
                        "prosedur_basligi", "Yıllık İzin Prosedürü",
                        "sorumlu_departman", "İnsan Kaynakları")));
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("İzin adımları...");

        chatMessageService.handleMessage("yıllık izin nasıl alınır", null);

        ArgumentCaptor<Map<String, String>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateResponseService).buildResponse(eq("izin_prosedur"), varsCaptor.capture());
        assertThat(varsCaptor.getValue())
                .containsEntry("prosedur_basligi", "Yıllık İzin Prosedürü")
                .containsEntry("sorumlu_departman", "İnsan Kaynakları")
                .containsEntry("departman", "Bilgi Teknolojileri");
        verify(templateResponseService, never()).buildFallbackResponse(any());
    }

    // A-5 / dokuman §2: İK intent'inin guncel versiyonu yoksa yanit fallback template'ine
    // duser; placeholder'li asil template render EDILMEZ.
    @Test
    void guncelVersiyonYoksaFallbackTemplateKullanilir() {
        var result = new IntentClassificationService.IntentResult(
                "izin_prosedur", 0.93, "yıllık izin nasıl alınır", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve("izin_prosedur"))
                .thenReturn(HrProcedureResolution.fallback());
        when(templateResponseService.buildFallbackResponse(any()))
                .thenReturn("Üzgünüm, şu anda bu soruya yanıt veremiyorum.");

        var response = chatMessageService.handleMessage("yıllık izin nasıl alınır", null);

        assertThat(response.reply()).isEqualTo("Üzgünüm, şu anda bu soruya yanıt veremiyorum.");
        verify(templateResponseService).buildFallbackResponse(any());
        verify(templateResponseService, never()).buildResponse(anyString(), any());
    }
}
