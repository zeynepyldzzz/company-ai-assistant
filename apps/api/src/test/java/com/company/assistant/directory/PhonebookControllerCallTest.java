package com.company.assistant.directory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-3: POST /phonebook/{extension}/call ucunun click-to-call tetikleme davranisini dogrular.
@SpringBootTest
@Transactional
class PhonebookControllerCallTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EmployeeRepository employeeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void triggerCall_validExtension_returnsAccepted() throws Exception {
        String extension = String.valueOf(System.nanoTime()).substring(0, 7);

        Employee employee = new Employee();
        employee.setName("Cagla " + UUID.randomUUID().toString().substring(0, 6));
        employee.setEmail(UUID.randomUUID() + "@test.company.com");
        employee.setPhone(extension);
        employeeRepository.saveAndFlush(employee);

        mockMvc.perform(post("/phonebook/{extension}/call", extension))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.extension").value(extension))
                .andExpect(jsonPath("$.status").value("triggered"));
    }

    @Test
    @WithMockUser
    void triggerCall_invalidExtension_returnsNotFoundWithMeaningfulError() throws Exception {
        String invalidExtension = "no-such-ext-" + UUID.randomUUID();

        mockMvc.perform(post("/phonebook/{extension}/call", invalidExtension))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EXTENSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(containsString(invalidExtension)));
    }
}
