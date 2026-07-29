package com.company.assistant.geocoding;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
