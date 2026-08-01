package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.Authentication;

import com.company.assistant.hr.HrProcedureResolution;
import com.company.assistant.hr.HrProcedureVariableResolver;
import com.company.assistant.menu.MenuVariableResolver;
import com.company.assistant.announcement.AnnouncementVariableResolver;
import com.company.assistant.directory.DepartmentVariableResolver;
import com.company.assistant.directory.DirectoryVariableResolver;
import com.company.assistant.survey.SurveyVariableResolver;
import com.company.assistant.schedule.ScheduleVariableResolver;
import com.company.assistant.shuttle.ShuttleVariableResolver;

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
    private MenuVariableResolver menuVariableResolver;
    @Mock
    private ShuttleVariableResolver shuttleVariableResolver;
    @Mock
    private ScheduleVariableResolver scheduleVariableResolver;
    @Mock
    private DirectoryVariableResolver directoryVariableResolver;
    @Mock
    private DepartmentVariableResolver departmentVariableResolver;
    @Mock
    private AnnouncementVariableResolver announcementVariableResolver;
    @Mock
    private SurveyVariableResolver surveyVariableResolver;
    @Mock
    private IntentSuggestionRepository suggestionRepository;
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

    // A-12 / FR-10: servis intent'inde canli servis degiskenleri ham mesajla uretilip
    // kullanici degiskenleriyle merge edilir.
    @Test
    @SuppressWarnings("unchecked")
    void servisIntentiIcinCanliDegiskenlerMergeEdilir() {
        var result = new IntentClassificationService.IntentResult(
                "servis_saatleri", 0.90, "servis kaçta kalkıyor", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of("kullanici_adi", "Mustafa"));
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(shuttleVariableResolver.resolve("servis_saatleri", "kadıköy servisi kaçta kalkıyor"))
                .thenReturn(Map.of("servis_saatleri", "Anadolu Yakasi - Kadikoy Hatti kalkış saatleri:\n• 07:00 Kadikoy Iskele"));
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("...");

        chatMessageService.handleMessage("kadıköy servisi kaçta kalkıyor", null);

        ArgumentCaptor<Map<String, String>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(templateResponseService).buildResponse(eq("servis_saatleri"), varsCaptor.capture());
        assertThat(varsCaptor.getValue())
                .containsEntry("kullanici_adi", "Mustafa")
                .containsEntry("servis_saatleri",
                        "Anadolu Yakasi - Kadikoy Hatti kalkış saatleri:\n• 07:00 Kadikoy Iskele");
    }

    // A-22 (#141): oneriler YALNIZCA intent bulunamadiginda doner. Karsilama mesaji sohbetin
    // basinda bir kez gorunur; kullanicinin gercekten kayboldugu an "anlamadim" yanitidir.
    @Test
    void intentBulunamazsaOnerilerDoner() {
        var result = new IntentClassificationService.IntentResult(
                IntentClassificationService.NO_INTENT, 0.41, "iyi günler", false);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Anlayamadım.");
        when(suggestionRepository.findSuggestions())
                .thenReturn(List.of(new ChatSuggestion("Bugün yemekte ne var?", "Bugün yemekte ne var?")));

        var response = chatMessageService.handleMessage("kedim neden miyavlıyor", null);

        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().get(0).question()).isEqualTo("Bugün yemekte ne var?");
    }

    // Eslesen yanitta oneri GOSTERILMEZ: kullanici zaten aradigini bulmus durumda,
    // chip listesi orada yalnizca gurultu olurdu.
    @Test
    void eslesenIntenttOnerilerBosDoner() {
        var result = new IntentClassificationService.IntentResult(
                "yemek_menusu", 0.91, "bugün yemekte ne var", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Mercimek çorbası.");

        var response = chatMessageService.handleMessage("bugün yemekte ne var", null);

        assertThat(response.suggestions()).isEmpty();
        verify(suggestionRepository, never()).findSuggestions();
    }

    @Test
    void intentinYonlendirmeButonuYanitaEklenir() {
        var result = new IntentClassificationService.IntentResult(
                "yemek_menusu", 0.91, "bugün yemekte ne var", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("Mercimek çorbası.");
        when(suggestionRepository.findActionByIntentName("yemek_menusu"))
                .thenReturn(Optional.of(new ChatAction("menu", "Aylık menüyü gör")));

        var response = chatMessageService.handleMessage("bugün yemekte ne var", null);

        assertThat(response.actions()).containsExactly(new ChatAction("menu", "Aylık menüyü gör"));
    }

    // Butonu olmayan intent'lerde alan BOS LISTE doner, null degil: istemci her yanitta
    // ayni sekilde okuyabilmeli.
    @Test
    void butonuOlmayanIntenttBosListeDoner() {
        var result = new IntentClassificationService.IntentResult(
                "izin_prosedur", 0.93, "yıllık izin nasıl alınır", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("İzin adımları...");
        when(suggestionRepository.findActionByIntentName("izin_prosedur")).thenReturn(Optional.empty());

        var response = chatMessageService.handleMessage("yıllık izin nasıl alınır", null);

        assertThat(response.actions()).isNotNull().isEmpty();
    }

    // A-13 / FR-63: calisma duzeni resolver'ina kimlik authentication ile gecirilir;
    // mesajda gecen isim degil, JWT'deki kimlik esas alinir.
    @Test
    void calismaDuzeniIntentiKimligiAuthenticationIleAlir() {
        var authentication = mock(Authentication.class);
        var result = new IntentClassificationService.IntentResult(
                "calisma_duzeni", 0.92, "yarın ofise gelmem gerekiyor mu", true);
        when(classificationService.classify(anyString())).thenReturn(result);
        when(classificationService.getThreshold()).thenReturn(0.68);
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(hrProcedureVariableResolver.resolve(anyString()))
                .thenReturn(HrProcedureResolution.notApplicable());
        when(scheduleVariableResolver.resolve("calisma_duzeni", "yarın ofiste miyim", authentication))
                .thenReturn(Map.of("calisma_duzenim", "Perşembe (30.07.2026) günü ofistesin."));
        when(templateResponseService.buildResponse(anyString(), any())).thenReturn("...");

        chatMessageService.handleMessage("yarın ofiste miyim", authentication);

        verify(scheduleVariableResolver).resolve("calisma_duzeni", "yarın ofiste miyim", authentication);
    }
}
