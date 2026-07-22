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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-1: /employees ucunun isme gore kismi eslesme (pg_trgm destekli fuzzy arama) davranisini dogrular.
@SpringBootTest
@Transactional
class DirectoryControllerSearchTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private MockMvc mockMvc;
    private String uniqueToken;
    private Employee employee;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        uniqueToken = UUID.randomUUID().toString().substring(0, 8);

        Department department = new Department();
        department.setName("Test Departmani " + uniqueToken);
        departmentRepository.saveAndFlush(department);

        employee = new Employee();
        employee.setName("Aylin " + uniqueToken + " Yildirim");
        employee.setEmail(uniqueToken + "@test.company.com");
        employee.setDepartment(department);
        employeeRepository.saveAndFlush(employee);
    }

    @Test
    @WithMockUser
    void partialNameSearch_caseInsensitive_returnsMatchingEmployee() throws Exception {
        String partial = uniqueToken.substring(0, 4).toUpperCase();

        mockMvc.perform(get("/employees").param("search", partial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(employee.getId()))
                .andExpect(jsonPath("$.data[0].name").value(employee.getName()));
    }

    @Test
    @WithMockUser
    void partialNameSearch_noMatch_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/employees").param("search", "no-such-name-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }
}
