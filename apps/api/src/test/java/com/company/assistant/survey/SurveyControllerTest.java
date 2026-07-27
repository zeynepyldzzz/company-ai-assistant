package com.company.assistant.survey;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.company.assistant.auth.JwtAuthFilter;
import com.company.assistant.auth.RestAccessDeniedHandler;
import com.company.assistant.auth.RestAuthenticationEntryPoint;
import com.company.assistant.config.SecurityConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-7 (#51) Test: FR-42/FR-43.
// Not: /feedback icin Authentication PARAMETRE OLARAK BILE ALINMAZ; bu yuzden
// "kimligi gormezden gelir" gibi bir test yazmak yerine (aksi mumkun olmadigi
// icin), gercek davranisi kanitliyoruz: submitFeedback SADECE govde ile cagrilir,
// hicbir employeeId parametresi YOKTUR (metot imzasinda yer almaz).
@WebMvcTest(SurveyController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SurveyService surveyService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void active_authOlmadan_401Doner() throws Exception {
        mockMvc.perform(get("/surveys/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void active_girisYapmisCalisan_200DonerVeListeyiGosterir() throws Exception {
        when(surveyService.getActiveSurveys()).thenReturn(List.of());

        mockMvc.perform(get("/surveys/active").with(user("42")))
                .andExpect(status().isOk());
    }

    // FR-42/FR-63 pattern: kimlik HER ZAMAN JWT'den (authentication.getName())
    // alinir, govdeden veya URL'den degil.
    @Test
    void postResponse_kimlik42_servis42IleCagrilir() throws Exception {
        mockMvc.perform(post("/surveys/7/responses")
                        .with(user("42")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"answers\": { \"q1\": \"evet\" } }"))
                .andExpect(status().isCreated());

        verify(surveyService).submitResponse(eq(7), eq(42), any());
    }

    @Test
    void postResponse_ayniGovdeKimlik99_servis99IleCagrilir() throws Exception {
        mockMvc.perform(post("/surveys/7/responses")
                        .with(user("99")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"answers\": { \"q1\": \"evet\" } }"))
                .andExpect(status().isCreated());

        verify(surveyService).submitResponse(eq(7), eq(99), any());
    }

    // FR-43 anonimlik: /feedback govdesinde employeeId gibi bir alan gonderilse
    // BILE, controller/service katmani bunu okumaz — SurveyService.submitFeedback
    // metodu zaten employeeId parametresi ALMAZ (bkz. SurveyService.java).
    @Test
    void postFeedback_authliCalisan_201DonerVeAnonimKaydedilir() throws Exception {
        mockMvc.perform(post("/feedback")
                        .with(user("42")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"surveyId\": 7, \"content\": \"harika bir anket\", \"employeeId\": 999 }"))
                .andExpect(status().isCreated());

        // Ispat: servis SADECE FeedbackRequest ile cagrildi; hicbir yerde
        // authentication/employeeId servise iletilmedi (metot bunu zaten kabul etmiyor).
        verify(surveyService).submitFeedback(any(FeedbackRequest.class));
        verify(surveyService, never()).submitResponse(any(), any(), any());
    }

    @Test
    void postFeedback_authOlmadan_401Doner() throws Exception {
        mockMvc.perform(post("/feedback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"test\" }"))
                .andExpect(status().isUnauthorized());
    }
}
