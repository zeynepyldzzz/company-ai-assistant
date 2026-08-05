package com.company.assistant.shuttle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ShuttleRouteRequest(
        @NotBlank(message = "Güzergah adı boş olamaz")
        String name,

        String plateNumber,

        String driverName,

        String driverPhone,

        @NotEmpty(message = "En az bir durak gereklidir")
        List<@Valid ShuttleStopRequest> stops,

        // B-31: haritada cizilip OSRM Match ile yola oturtulmus rota noktalari.
        // Opsiyonel - girilmezse guzergah eski OSRM-route-arasi-duraklar
        // fallback'ine duser (getRouteGeometry).
        List<@Valid RoutePointRequest> geometryPoints
) {}
