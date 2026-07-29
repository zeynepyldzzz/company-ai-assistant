package com.company.assistant.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.company.assistant.directory.DepartmentService;
import com.company.assistant.directory.DirectoryService;
import com.company.assistant.shuttle.ShuttleService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IntentClassificationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private EmbeddingClient embeddingClient;
    private IntentClassificationService service;

    private static final double THRESHOLD = 0.68;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        embeddingClient = mock(EmbeddingClient.class);
        // Kural eslestiricinin KENDISI gercek: plaka deseni saf mantik ve testin konusu.
        // Varlik kurallari icin gereken servisler bos veri doner; onlarin kendi testleri var.
        ShuttleService shuttleService = mock(ShuttleService.class);
        DirectoryService directoryService = mock(DirectoryService.class);
        DepartmentService departmentService = mock(DepartmentService.class);
        lenient().when(shuttleService.getAllRoutes()).thenReturn(List.of());
        lenient().when(departmentService.getDepartmentNames()).thenReturn(List.of());
        lenient().when(directoryService.existsActiveEmployeeNamed(any())).thenReturn(false);
        service = new IntentClassificationService(jdbcTemplate, embeddingClient,
                new RuleBasedIntentMatcher(shuttleService, directoryService, departmentService),
                THRESHOLD);
    }

    private void mockDbBestMatch(String intent, String phrase, double similarity) {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("name", intent, "phrase", phrase, "similarity", similarity)));
    }

    // A-17 (#124): plaka embedding ile siniflandirilamiyordu — "34 SR 101" 0.463 benzerlikle
    // "merhaba" cumlesine en yakin cikiyordu. Kural eslesince embedding hic cagrilmamali.
    @Test
    void plakaKuralIleServisIntentineGider() {
        var result = service.classify("34 SR 101");

        assertThat(result.matched()).isTrue();
        assertThat(result.intent()).isEqualTo("servis_guzergah");
        verifyNoInteractions(embeddingClient, jdbcTemplate);
    }

    @Test
    void bosluksuzPlakaDaKuralaTakilir() {
        assertThat(service.classify("34sr101 nerede").intent()).isEqualTo("servis_guzergah");
    }

    // Kural yalnizca plaka bicimine uymali; siradan sorular embedding yoluna gitmeli.
    @Test
    void plakaOlmayanSoruEmbeddingYolunaGider() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        mockDbBestMatch("yemek_menusu", "bugün yemekte ne var", 0.90);

        var result = service.classify("bugün yemekte ne var");

        assertThat(result.intent()).isEqualTo("yemek_menusu");
        verify(embeddingClient).embed("bugün yemekte ne var");
    }

    // A-17 (#124): normalizasyon yokken "Selamlar" 0.513, "selamlar" 0.797 aliyordu. Soru
    // embedding'e kucuk harfe cevrilmis ve trim'lenmis olarak gitmeli; ayni metot
    // IntentSeedRunner'da da kullanilir, aksi halde iki taraf ayni uzayda olmaz.
    @Test
    void soruEmbeddingeNormallestirilmisGider() {
        when(embeddingClient.embed("selamlar")).thenReturn(new float[]{0.1f});
        mockDbBestMatch("selamlama", "merhaba", 0.79);

        var result = service.classify("  Selamlar  ");

        assertThat(result.matched()).isTrue();
        assertThat(result.intent()).isEqualTo("selamlama");
        verify(embeddingClient).embed("selamlar");
    }

    // Turkce karakterler BILEREK katlanmaz: bge-m3 cok dilli, "calisma"ya cevirmek modele
    // bozuk kelime vermek olur.
    @Test
    void turkceKarakterlerKatlanmaz() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        mockDbBestMatch("calisma_duzeni", "bu hafta kimler ofiste", 0.80);

        service.classify("Çalışma Düzenim");

        verify(embeddingClient).embed("çalışma düzenim");
    }

    @Test
    void esikUstuBenzerlikteIntentDoner() {
        when(embeddingClient.embed("bugün yemekte ne var")).thenReturn(new float[]{0.1f, 0.2f});
        mockDbBestMatch("yemek_menusu", "bugün yemekte ne var", 0.85);

        var result = service.classify("bugün yemekte ne var");

        assertThat(result.matched()).isTrue();
        assertThat(result.intent()).isEqualTo("yemek_menusu");
        assertThat(result.similarity()).isEqualTo(0.85);
    }

    @Test
    void esikAltiBenzerlikteFallbackTetiklenir() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        mockDbBestMatch("duyurular", "yeni duyuru var mı", 0.45);

        var result = service.classify("bitcoin fiyatı ne kadar");

        assertThat(result.matched()).isFalse();
        assertThat(result.intent()).isEqualTo(IntentClassificationService.NO_INTENT);
    }

    @Test
    void tamEsikDegerindeIntentDoner() {
        // sinir durumu: similarity == threshold -> esik "altinda" degil, eslesme sayilir
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        mockDbBestMatch("anket", "aktif anket var mı", THRESHOLD);

        var result = service.classify("anket var mı");

        assertThat(result.matched()).isTrue();
    }

    @Test
    void embeddingServisiHataVerirseFallbackDoner() {
        when(embeddingClient.embed(anyString())).thenThrow(new IllegalStateException("Ollama kapali"));

        var result = service.classify("merhaba");

        assertThat(result.matched()).isFalse();
        assertThat(result.intent()).isEqualTo(IntentClassificationService.NO_INTENT);
    }

    @Test
    void ornekTablosuBossaFallbackDoner() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        var result = service.classify("merhaba");

        assertThat(result.matched()).isFalse();
    }
}