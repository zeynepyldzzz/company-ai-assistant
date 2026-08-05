package com.company.assistant.geocoding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// B-16: serbest metin adres -> enlem/boylam (Nominatim proxy). Gercek Nominatim'e
// istek atmamak icin yerel bir HTTP sunucusu ile sahte yanit doner.
class GeocodingServiceTest {

    private HttpServer stubServer;
    private GeocodingService service;

    @BeforeEach
    void setUp() throws IOException {
        stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stubServer.start();
        String baseUrl = "http://localhost:" + stubServer.getAddress().getPort();
        service = new GeocodingService(baseUrl, "test-agent/1.0");
    }

    @AfterEach
    void tearDown() {
        stubServer.stop(0);
    }

    @Test
    void adresEnlemBoylamaCozulur() throws IOException {
        String[] capturedUserAgent = new String[1];
        String[] capturedQuery = new String[1];
        stubServer.createContext("/search", exchange -> {
            capturedUserAgent[0] = exchange.getRequestHeaders().getFirst("User-Agent");
            capturedQuery[0] = exchange.getRequestURI().getRawQuery();
            byte[] body = "[{\"lat\":\"40.9906\",\"lon\":\"29.0274\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        GeocodingResult result = service.geocode("Kadıköy");

        assertThat(result.lat()).isEqualTo(40.9906);
        assertThat(result.lng()).isEqualTo(29.0274);
        assertThat(capturedUserAgent[0]).isEqualTo("test-agent/1.0");
        assertThat(URLDecoder.decode(capturedQuery[0], StandardCharsets.UTF_8)).contains("q=Kadıköy");
    }

    @Test
    void suggestIzmirManisaDisindakiSonuclariFiltrelerVeCokluSonucDoner() throws IOException {
        String[] capturedQuery = new String[1];
        stubServer.createContext("/search", exchange -> {
            capturedQuery[0] = exchange.getRequestURI().getRawQuery();
            byte[] body = ("[" +
                    "{\"lat\":\"40.9906\",\"lon\":\"29.0274\",\"display_name\":\"Kadıköy, İstanbul\",\"address\":{\"province\":\"İstanbul\"}}," +
                    "{\"lat\":\"38.4237\",\"lon\":\"27.1428\",\"display_name\":\"Alsancak, İzmir\",\"address\":{\"province\":\"İzmir\"}}," +
                    "{\"lat\":\"38.6191\",\"lon\":\"27.4289\",\"display_name\":\"Şehzadeler, Manisa\",\"address\":{\"province\":\"Manisa\"}}" +
                    "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        List<AddressSuggestion> suggestions = service.suggest("Kadıköy", 5);

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).label()).isEqualTo("Alsancak, İzmir");
        assertThat(suggestions.get(0).lat()).isEqualTo(38.4237);
        assertThat(suggestions.get(0).lng()).isEqualTo(27.1428);
        assertThat(suggestions.get(1).label()).isEqualTo("Şehzadeler, Manisa");
        String decodedQuery = URLDecoder.decode(capturedQuery[0], StandardCharsets.UTF_8);
        assertThat(decodedQuery).contains("limit=20", "addressdetails=1", "countrycodes=tr");
    }

    @Test
    void suggestFiltrelenmisSonuclaraIstenenLimitiUygular() throws IOException {
        stubServer.createContext("/search", exchange -> {
            byte[] body = ("[" +
                    "{\"lat\":\"38.4237\",\"lon\":\"27.1428\",\"display_name\":\"Alsancak, İzmir\",\"address\":{\"province\":\"İzmir\"}}," +
                    "{\"lat\":\"38.6191\",\"lon\":\"27.4289\",\"display_name\":\"Şehzadeler, Manisa\",\"address\":{\"province\":\"Manisa\"}}" +
                    "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        List<AddressSuggestion> suggestions = service.suggest("a", 1);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).label()).isEqualTo("Alsancak, İzmir");
    }

    @Test
    void suggestProvinceAlaniYoksaStateAlaninaYedeklenir() throws IOException {
        stubServer.createContext("/search", exchange -> {
            byte[] body = ("[" +
                    "{\"lat\":\"38.4237\",\"lon\":\"27.1428\",\"display_name\":\"Alsancak, İzmir\",\"address\":{\"state\":\"İzmir\"}}" +
                    "]").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThat(service.suggest("Alsancak", 5)).hasSize(1);
    }

    @Test
    void suggestSonucYoksaBosListeDoner() throws IOException {
        stubServer.createContext("/search", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThat(service.suggest("asdkjfhaskjdfh", 5)).isEmpty();
    }

    @Test
    void taninmayanAdresteAnlamliHataFirlatir() throws IOException {
        stubServer.createContext("/search", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThatThrownBy(() -> service.geocode("asdkjfhaskjdfh"))
                .isInstanceOf(AddressNotFoundException.class);
    }
}
