package com.banking.service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@ActiveProfiles({"test", "test-tls"})
class TlsConnectivityIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // Use REQUIRE (not VERIFY_CA) for test simplicity — Testcontainers MySQL has no custom CA
        registry.add("spring.datasource.url",
                () -> mysql.getJdbcUrl() + "?sslMode=REQUIRED");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void httpsHealthEndpointReturns200() throws Exception {
        SSLContext sslContext = buildSslContext();

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://localhost:8443/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void plainHttpOnTlsPortFails() throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8443/health"))
                .GET()
                .build();

        // Tomcat responds with HTTP 400 when it receives a plain-text request on a TLS port.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private static SSLContext buildSslContext() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream is = TlsConnectivityIT.class.getResourceAsStream("/ssl/test-truststore.jks")) {
            if (is == null) {
                throw new IllegalStateException(
                        "test-truststore.jks not found on classpath. Run scripts/generate-certs.sh first.");
            }
            trustStore.load(is, "changeit".toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }
}
