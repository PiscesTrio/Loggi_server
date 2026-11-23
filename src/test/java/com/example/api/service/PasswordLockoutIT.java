package com.example.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.dto.LoginDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Password sign-in stops answering after too many wrong guesses, against a real Redis.
 *
 * <p>Integration rather than unit for the reason VerificationCodeIT gives: with a mocked {@code
 * StringRedisTemplate} every assertion here passes against a limiter that counts nothing. The
 * account is the seeded demo administrator, so the correct password is a real hash check.
 */
@Testcontainers
@SpringBootTest
class PasswordLockoutIT {

    private static final String EMAIL = "demo@loggi.example";
    private static final String PASSWORD = "demo1234";
    private static final int MAX_ATTEMPTS = 3;

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("app.verification.max-attempts", () -> MAX_ATTEMPTS);
    }

    @Autowired AdminService adminService;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void clearState() {
        redis.delete(redis.keys("login:*"));
    }

    private static LoginDto login(String email, String password) {
        LoginDto dto = new LoginDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private ErrorCode refusal(String email, String password) {
        try {
            adminService.loginByPassword(login(email, password));
            return null;
        } catch (BizException e) {
            return e.getErrorCode();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("After the limit of wrong passwords, even the right one is refused")
    void lockout_refusesTheCorrectPasswordToo() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThat(refusal(EMAIL, "wrong-" + i)).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        // A lock that the right guess opens would tell the guesser they had found it.
        assertThat(refusal(EMAIL, PASSWORD)).isEqualTo(ErrorCode.LOGIN_ATTEMPTS_EXHAUSTED);
        assertThat(redis.getExpire("login:lock:" + EMAIL)).isPositive();
    }

    @Test
    @DisplayName("A correct password clears earlier mistakes")
    void success_resetsTheCount() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            refusal(EMAIL, "wrong-" + i);
        }
        assertThat(refusal(EMAIL, PASSWORD)).isNull();

        // The budget is whole again: one more mistake does not lock.
        assertThat(refusal(EMAIL, "wrong-again")).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(refusal(EMAIL, PASSWORD)).isNull();
    }

    @Test
    @DisplayName("Changing the case of the address does not buy more guesses")
    void caseVariants_shareOneCounter() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            refusal(i % 2 == 0 ? EMAIL : EMAIL.toUpperCase(), "wrong-" + i);
        }

        assertThatThrownBy(() -> adminService.loginByPassword(login(EMAIL, PASSWORD)))
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ATTEMPTS_EXHAUSTED);
    }
}
