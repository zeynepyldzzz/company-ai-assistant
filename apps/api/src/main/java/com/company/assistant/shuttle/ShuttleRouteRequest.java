package com.company.assistant.shuttle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ShuttleRouteRequest(
        // A-44 (#219): sinirlarin kaynagi V1__init.sql (name VARCHAR(150), plate_number
        // VARCHAR(20)) ve V29 (driver_name VARCHAR(255), driver_phone VARCHAR(50)).
        @NotBlank(message = "Güzergah adı boş olamaz")
        @Size(max = 150, message = "Güzergah adı 150 karakteri aşamaz")
        String name,

        @Size(max = 20, message = "Plaka 20 karakteri aşamaz")
        String plateNumber,

        @Size(max = 255, message = "Sürücü adı 255 karakteri aşamaz")
        String driverName,

        @Size(max = 50, message = "Sürücü telefonu 50 karakteri aşamaz")
        String driverPhone,

        @NotEmpty(message = "En az bir durak gereklidir")
        List<@Valid ShuttleStopRequest> stops,

        // B-31: haritada cizilip OSRM Match ile yola oturtulmus rota noktalari.
        // Opsiyonel - girilmezse guzergah eski OSRM-route-arasi-duraklar
        // fallback'ine duser (getRouteGeometry).
        List<@Valid RoutePointRequest> geometryPoints
) {}
