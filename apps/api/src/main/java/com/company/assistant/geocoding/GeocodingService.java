package com.company.assistant.geocoding;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * B-16: serbest metin konum (semt/adres) -> enlem/boylam.
 * Saglayici: OpenStreetMap Nominatim. Kullanim politikasi geregi (https://operations.osmfoundation.org/policies/nominatim/)
 * her istekte tanimlayici bir User-Agent gonderilmesi zorunlu.
 */
@Service
public class GeocodingService {

    private final RestClient restClient;

    public GeocodingService(
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.user-agent}") String userAgent) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    public GeocodingResult geocode(String query) {
        List<NominatimResult> results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<NominatimResult>>() {});

        if (results == null || results.isEmpty()) {
            throw new AddressNotFoundException("Adres bulunamadi: " + query);
        }

        NominatimResult first = results.get(0);
        return new GeocodingResult(Double.parseDouble(first.lat()), Double.parseDouble(first.lon()));
    }

    record NominatimResult(String lat, String lon) {}
}
