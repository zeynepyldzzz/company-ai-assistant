package com.company.assistant.geocoding;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // A-33: oneri dropdown'u sadece servis guzergahlarinin bulundugu Izmir/Manisa
    // illeriyle sinirlandirilir; Nominatim filtre sonrasi yeterli sonuc kalsin diye
    // istenen limitten daha fazla ham sonuc cekilir.
    private static final Set<String> ALLOWED_PROVINCES = Set.of("izmir", "manisa");
    private static final int RAW_SUGGESTION_LIMIT = 20;
    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

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

    /**
     * A-33: kullanici yazarken oneri listesi icin Nominatim'den birden fazla
     * sonuc doner (geocode() tek/en iyi sonucu doner, bu oneri listesi doner).
     */
    public List<AddressSuggestion> suggest(String query, int limit) {
        List<NominatimResult> results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("limit", RAW_SUGGESTION_LIMIT)
                        .queryParam("addressdetails", 1)
                        .queryParam("countrycodes", "tr")
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<NominatimResult>>() {});

        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(this::isInAllowedProvince)
                .limit(limit)
                .map(r -> new AddressSuggestion(r.displayName(), Double.parseDouble(r.lat()), Double.parseDouble(r.lon())))
                .toList();
    }

    private boolean isInAllowedProvince(NominatimResult result) {
        if (result.address() == null) {
            return false;
        }
        // Nominatim Turkiye icin il adini "province" alaninda doner; "state" bazi
        // ulkeler/surumler icin kullanildigindan yedek olarak kontrol edilir.
        String province = result.address().province() != null
                ? result.address().province()
                : result.address().state();
        return province != null && ALLOWED_PROVINCES.contains(province.toLowerCase(TURKISH));
    }

    record NominatimResult(
            String lat,
            String lon,
            @JsonProperty("display_name") String displayName,
            NominatimAddress address) {}

    record NominatimAddress(String province, String state) {}
}
