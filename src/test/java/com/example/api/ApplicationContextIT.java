package com.example.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most direct smoke test for a framework upgrade: boot the <b>whole</b> ApplicationContext.
 *
 * <p>Other tests each cover one slice — @WebMvcTest web, @DataJpaTest JPA, the two Mockito unit
 * tests never start Spring — so before S01 <b>nothing asserted</b> that AOP aspects, mail,
 * background tasks and the security filter chain all still wire together across the
 * major-version jump.
 *
 * <p>RANDOM_PORT, not the default MOCK: it <b>actually starts the embedded web container</b>,
 * making the S01 acceptance item "`spring-boot:run` starts cleanly" a per-build regression.
 *
 * <p>*IT, not *Test: a full context needs a real datasource — failsafe plus Testcontainers.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationContextIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // match production
        // Required, not incidental: JwtTokenUtil refuses to construct on the CHANGE_ME
        // placeholder, so from S02 on the context cannot start without a real secret. That
        // this line is necessary IS the fail-fast working.
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
    }

    @Autowired ApplicationContext context;

    @Test
    @DisplayName("The whole application context loads on the upgraded baseline")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("The security filter chain is still assembled by the rewritten configuration")
    void securityFilterChainIsRegistered() {
        // S01 rewrote WebSecurityConfigurerAdapter into a SecurityFilterChain bean.
        // Assert only that the chain got assembled — pinning down its rules belongs to S02.
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
    }
}
