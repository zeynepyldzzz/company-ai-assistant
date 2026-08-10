package com.company.assistant.routing;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// B-23: OSRM'den gercek yol mesafesi/suresi. Gercek OSRM sunucusuna istek
// atmamak icin yerel bir HTTP sunucusu ile sahte yanit doner.
class RoutingServiceTest {

    private HttpServer stubServer;
    private RoutingService service;

    @BeforeEach
    void setUp() throws IOException {
        stubServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        stubServer.start();
        String baseUrl = "http://localhost:" + stubServer.getAddress().getPort();
        service = new RoutingService(baseUrl);
    }

    @AfterEach
    void tearDown() {
        stubServer.stop(0);
    }

    @Test
    void osrmYanitindanMesafeVeSureCozulur() throws IOException {
        stubServer.createContext("/route/v1/driving/29.03,40.98;29.05,41.0", exchange -> {
            byte[] body = ("{\"code\":\"Ok\",\"routes\":[{\"distance\":31400.0,\"duration\":2520.0}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<RouteResult> result = service.route(40.98, 29.03, 41.0, 29.05);

        assertThat(result).isPresent();
        assertThat(result.get().distanceKm()).isEqualTo(31.4);
        assertThat(result.get().durationMinutes()).isEqualTo(42);
    }

    @Test
    void rotaBulunamazsaBosDoner() throws IOException {
        stubServer.createContext("/route/v1/driving/", exchange -> {
            byte[] body = "{\"code\":\"NoRoute\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<RouteResult> result = service.route(40.98, 29.03, 41.0, 29.05);

        assertThat(result).isEmpty();
    }

    @Test
    void sunucuHataDonerseBosDoner() throws IOException {
        stubServer.createContext("/route/v1/driving/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        Optional<RouteResult> result = service.route(40.98, 29.03, 41.0, 29.05);

        assertThat(result).isEmpty();
    }

    @Test
    void sunucuErisilemezseBosDoner() {
        stubServer.stop(0);

        Optional<RouteResult> result = service.route(40.98, 29.03, 41.0, 29.05);

        assertThat(result).isEmpty();
    }

    @Test
    void geometriYanitindanKoordinatlarCozulur() throws IOException {
        stubServer.createContext("/route/v1/driving/29.03,40.98;29.04,40.99;29.05,41.0", exchange -> {
            byte[] body = ("{\"code\":\"Ok\",\"routes\":[{\"geometry\":{\"coordinates\":"
                    + "[[29.03,40.98],[29.035,40.985],[29.05,41.0]]}}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.routeGeometry(List.of(
                new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04), new Coordinate(41.0, 29.05)));

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(
                new Coordinate(40.98, 29.03), new Coordinate(40.985, 29.035), new Coordinate(41.0, 29.05));
    }

    @Test
    void tekNoktaIcinGeometriBosDoner() {
        Optional<List<Coordinate>> result = service.routeGeometry(List.of(new Coordinate(40.98, 29.03)));

        assertThat(result).isEmpty();
    }

    @Test
    void geometriSunucuHataDonerseBosDoner() throws IOException {
        stubServer.createContext("/route/v1/driving/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.routeGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(41.0, 29.05)));

        assertThat(result).isEmpty();
    }

    @Test
    void matchYanitindanKoordinatlarCozulur() throws IOException {
        java.util.concurrent.atomic.AtomicReference<String> capturedQuery = new java.util.concurrent.atomic.AtomicReference<>();
        stubServer.createContext("/match/v1/driving/29.03,40.98;29.04,40.99", exchange -> {
            capturedQuery.set(exchange.getRequestURI().getQuery());
            byte[] body = ("{\"code\":\"Ok\",\"matchings\":[{\"geometry\":{\"coordinates\":"
                    + "[[29.03,40.98],[29.035,40.985],[29.04,40.99]]}}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.matchGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04)));

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(
                new Coordinate(40.98, 29.03), new Coordinate(40.985, 29.035), new Coordinate(40.99, 29.04));
        assertThat(capturedQuery.get()).contains("radiuses=30;30");
    }

    @Test
    void matchEslesemezseBosDoner() throws IOException {
        stubServer.createContext("/match/v1/driving/", exchange -> {
            byte[] body = "{\"code\":\"NoMatch\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.matchGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04)));

        assertThat(result).isEmpty();
    }

    @Test
    void matchBosEslesmeListesiVarsaBosDoner() throws IOException {
        stubServer.createContext("/match/v1/driving/", exchange -> {
            byte[] body = "{\"code\":\"Ok\",\"matchings\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.matchGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04)));

        assertThat(result).isEmpty();
    }

    @Test
    void matchSunucuHataDonerseBosDoner() throws IOException {
        stubServer.createContext("/match/v1/driving/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        Optional<List<Coordinate>> result = service.matchGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04)));

        assertThat(result).isEmpty();
    }

    @Test
    void matchSunucuErisilemezseBosDoner() {
        stubServer.stop(0);

        Optional<List<Coordinate>> result = service.matchGeometry(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04)));

        assertThat(result).isEmpty();
    }

    @Test
    void matchTekNoktaIcinBosDonerAgSorgusuYapilmaz() {
        Optional<List<Coordinate>> result = service.matchGeometry(List.of(new Coordinate(40.98, 29.03)));

        assertThat(result).isEmpty();
    }
}
