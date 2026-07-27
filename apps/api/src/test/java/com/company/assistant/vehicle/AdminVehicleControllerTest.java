package com.company.assistant.vehicle;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-8/B-T3: POST/PUT /admin/vehicles, PUT /admin/vehicles/{id}/maintenance-status (FR-74, 41)
// Yalnizca fleet_admin / system_admin erisebilir.
@WebMvcTest(AdminVehicleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminVehicleControllerTest {

    private static final String VEHICLE_REQUEST_JSON = """
            {"plate": "34 ABC 123", "model": "Renault Megane", "maintenanceStatus": "available"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminVehicleService adminVehicleService;

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
    void fleetAdmin_yeniAracEkleyebilir() throws Exception {
        when(adminVehicleService.createVehicle(any())).thenReturn(
                new VehicleResponse(vehicle(1, "34 ABC 123", MaintenanceStatus.AVAILABLE)));

        mockMvc.perform(post("/admin/vehicles")
                        .with(user("filo").roles("ADMIN", "FLEET_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("34 ABC 123"));
    }

    @Test
    void systemAdmin_deErisebilir() throws Exception {
        when(adminVehicleService.createVehicle(any())).thenReturn(
                new VehicleResponse(vehicle(1, "34 ABC 123", MaintenanceStatus.AVAILABLE)));

        mockMvc.perform(post("/admin/vehicles")
                        .with(user("sistem").roles("ADMIN", "SYSTEM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void baskaAdminAltRolu_403Doner() throws Exception {
        // ROLE_ADMIN'e sahip (filter-chain seviyesini gecer) ama fleet_admin/system_admin
        // degil - method-level @PreAuthorize reddetmeli.
        mockMvc.perform(post("/admin/vehicles")
                        .with(user("hr").roles("ADMIN", "HR_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(post("/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plakaBossa_400ValidationHatasiDoner() throws Exception {
        mockMvc.perform(post("/admin/vehicles")
                        .with(user("filo").roles("ADMIN", "FLEET_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plate": "", "model": "Renault Megane"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void fleetAdmin_aracGuncelleyebilir() throws Exception {
        when(adminVehicleService.updateVehicle(eq(1), any())).thenReturn(
                new VehicleResponse(vehicle(1, "34 ABC 123", MaintenanceStatus.AVAILABLE)));

        mockMvc.perform(put("/admin/vehicles/1")
                        .with(user("filo").roles("ADMIN", "FLEET_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("34 ABC 123"));
    }

    @Test
    void fleetAdmin_bulunamayanAraciGuncellerse404Doner() throws Exception {
        when(adminVehicleService.updateVehicle(eq(999), any()))
                .thenThrow(new VehicleNotFoundException("Araç bulunamadı, id: 999"));

        mockMvc.perform(put("/admin/vehicles/999")
                        .with(user("filo").roles("ADMIN", "FLEET_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEHICLE_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
    }

    @Test
    void fleetAdmin_bakimDurumunuGuncelleyebilir() throws Exception {
        when(adminVehicleService.updateMaintenanceStatus(eq(1), any())).thenReturn(
                new VehicleResponse(vehicle(1, "34 ABC 123", MaintenanceStatus.MAINTENANCE)));

        mockMvc.perform(put("/admin/vehicles/1/maintenance-status")
                        .with(user("filo").roles("ADMIN", "FLEET_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maintenanceStatus\": \"maintenance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceStatus").value("maintenance"));
    }

    private Vehicle vehicle(Integer id, String plate, MaintenanceStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setPlate(plate);
        vehicle.setMaintenanceStatus(status);
        return vehicle;
    }
}
