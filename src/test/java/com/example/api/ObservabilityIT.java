package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Health and API documentation, asserted over real HTTP.
 *
 * <p>Both are new in S10 and both have a failure mode that a green build would otherwise hide.
 * Health has to answer without a token, because an orchestrator probes it before the application
 * can issue one - and it has to answer UP, which it did not at first: Actuator's mail indicator
 * authenticates against SMTP, the credentials here are placeholders by design, and the failing
 * indicator dragged the aggregate to DOWN. A readiness probe would have kept the container out of
 * service forever, on a fresh clone, with the application itself working perfectly.
 *
 * <p>The rest of /actuator must stay closed. Exposing health publicly is a decision; letting
 * /actuator/metrics follow it out is an accident, and the two are one line apart in the security
 * configuration.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityIT {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    /**
     * Redis has to be here, and finding out why was the point.
     *
     * <p>Without it the health endpoint answers DOWN - correctly. Redis holds the one-time codes
     * and their rate-limit counters, so an application that cannot reach it cannot serve the e-mail
     * login path, and saying UP would be a lie. That is the difference between this indicator and
     * the mail one that had to be switched off: an unreachable SMTP server is a third party being
     * unavailable, while an unreachable Redis is this application being unable to do part of its
     * job.
     */
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
    }

    @LocalServerPort int port;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                // Statuses are the assertion here, so nothing may throw on any of them.
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {})
                .build();
    }

    @Test
    @DisplayName("Health answers UP without a token, because a probe has no way to get one")
    void healthIsPublicAndUp() {
        ResponseEntity<String> response =
                client().get().uri("/actuator/health").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName(
            "Health names no component, so an anonymous caller learns nothing about the internals")
    void healthDoesNotDescribeItsComponents() {
        String body = client().get().uri("/actuator/health").retrieve().body(String.class);

        // show-details: never. The detailed view lists which database or broker is failing,
        // and this endpoint is answered without authentication.
        assertThat(body).doesNotContain("components", "db", "redis", "diskSpace");
    }

    @Test
    @DisplayName("The rest of /actuator is not public just because health is")
    void otherActuatorEndpointsStayProtected() {
        assertThat(
                        client().get()
                                .uri("/actuator/metrics")
                                .retrieve()
                                .toBodilessEntity()
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(
                        client().get()
                                .uri("/actuator/env")
                                .retrieve()
                                .toBodilessEntity()
                                .getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("The OpenAPI document is served and describes the real API")
    void openApiDocumentIsServed() {
        ResponseEntity<String> response =
                client().get().uri("/v3/api-docs").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        // Named paths rather than a count: a count stays green while the document describes
        // something else entirely.
        assertThat(body)
                .contains("/api/distribution")
                .contains("/api/admin/login/password")
                .contains("/api/inventory/in");
    }

    @Test
    @DisplayName("The document declares the bearer scheme, which no scanner could have inferred")
    void openApiDocumentDescribesAuthentication() {
        String body = client().get().uri("/v3/api-docs").retrieve().body(String.class);

        // The JWT filter lives in the security chain, not on the controllers, so nothing in
        // the code says these endpoints need a token. Without the declaration the document
        // describes an API that appears to need no credentials, and every request tried from
        // Swagger UI answers 401 with no hint why.
        assertThat(body).contains("bearerAuth").contains("\"bearerFormat\":\"JWT\"");
    }

    @Test
    @DisplayName("Sale and Employee are documented as API-only, not left looking unfinished")
    void apiOnlyResourcesAreLabelledAsSuch() {
        String body = client().get().uri("/v3/api-docs").retrieve().body(String.class);

        // No client calls either. Keeping them was a decision, and an API-only resource and
        // an unfinished one look identical in a repository unless someone writes it down -
        // so it is written down here, in the artefact a reader actually opens.
        assertThat(body).contains("Sales (API only)").contains("Employees (API only)");
    }

    @Test
    @DisplayName("The document describes the envelope the server actually sends")
    void openApiDocumentDescribesTheEnvelope() {
        String body = client().get().uri("/v3/api-docs").retrieve().body(String.class);

        // springdoc derives a response schema from the controller's return type, and this API
        // does not send that: the envelope is applied by a ResponseBodyAdvice after the method
        // returns. Left alone, the document claimed GET /api/commodity answers with an array
        // of CommodityVo while the server answers with an object containing one - so a client
        // generated from it would fail to parse every response, and "OpenAPI as the single
        // source of truth" would be a claim rather than a fact.
        assertThat(body).contains("ResponseResultCommodityVoList");
        assertThat(body).contains("ResponseResultPageVoSystemLogVo");
    }

    @Test
    @DisplayName("A 204 is documented with no body, matching what it sends")
    void noContentIsDocumentedWithoutAnEnvelope() {
        String body = client().get().uri("/v3/api-docs").retrieve().body(String.class);

        // The wrapping has to skip 204 for the same reason the advice does. A response
        // documented as carrying an envelope while carrying nothing is the same class of
        // untruth in the other direction.
        assertThat(body).doesNotContain("ResponseResultVoidList");
    }
}
