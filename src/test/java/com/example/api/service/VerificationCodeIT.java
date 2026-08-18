package com.example.api.service;

import com.example.api.exception.BizException;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one-time code flow, against a real Redis and a real SMTP server.
 *
 * <p>Written as an integration test rather than a unit test on purpose. Every property that
 * matters here is a property of the interaction: that the message is genuinely handed to a
 * mail server, that the value in the store is not the value in the message, that a second
 * send inside the cooldown is refused, that the sixth wrong guess locks the address. Mocking
 * {@code StringRedisTemplate} would let all four assertions pass against a service that does
 * nothing, which is roughly the state this slice found the code in — {@code mailSender.send}
 * had been commented out and no test noticed for the life of the project.
 *
 * <p>GreenMail is an in-process SMTP server, so "was the mail sent" is answered by reading
 * the mail.
 */
@Testcontainers
@SpringBootTest
class VerificationCodeIT {

    private static final String EMAIL = "code-it@loggi.example";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static final String SMTP_USER = "loggi@loggi.example";
    private static final String SMTP_PASSWORD = "smtp-test-password";

    // The mailbox has to exist. JavaMailSenderImpl authenticates whenever spring.mail.username
    // is non-empty, regardless of mail.smtp.auth, and an empty username is not an option here
    // because the same property supplies the From address.
    @RegisterExtension
    static final GreenMailExtension SMTP = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser(SMTP_USER, SMTP_PASSWORD));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");

        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // GreenMail speaks plain SMTP on a random-ish port; the production config points at
        // smtp.qq.com over implicit SSL, which a test must not dial.
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
        r.add("spring.mail.username", () -> SMTP_USER);
        r.add("spring.mail.password", () -> SMTP_PASSWORD);
        r.add("spring.mail.properties.mail.smtp.ssl.enable", () -> "false");
        r.add("spring.mail.properties.mail.smtp.auth", () -> "false");
    }

    @Autowired EmailService emailService;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void clearState() {
        // Each test starts from an empty store; the cooldown alone would otherwise make the
        // second test in the class fail for a reason that has nothing to do with it.
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private String sendAndReadCode() throws Exception {
        emailService.sendVerificationCode(EMAIL);
        assertThat(SMTP.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage[] received = SMTP.getReceivedMessages();
        String body = received[received.length - 1].getContent().toString();
        Matcher m = Pattern.compile("(\\d{6})").matcher(body);
        assertThat(m.find()).as("the message must contain a six-digit code").isTrue();
        return m.group(1);
    }

    @Test
    @DisplayName("The code is actually mailed, and the store holds a hash rather than the code")
    void code_isSentAndStoredHashed() throws Exception {
        String code = sendAndReadCode();

        // The whole defect: this used to be the constant "123456", and the send was
        // commented out, so the code was neither secret nor delivered.
        assertThat(code).isNotEqualTo("123456");

        String stored = redis.opsForValue().get("verification:code:" + EMAIL);
        assertThat(stored).isNotNull().isNotEqualTo(code);
        // SHA-256 hex.
        assertThat(stored).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(redis.getExpire("verification:code:" + EMAIL)).isPositive();

        assertThat(emailService.checkVerificationCode(EMAIL, code)).isTrue();
    }

    @Test
    @DisplayName("A code works once; the second use of the same code fails")
    void code_isSingleUse() throws Exception {
        String code = sendAndReadCode();

        assertThat(emailService.checkVerificationCode(EMAIL, code)).isTrue();
        // Without deleting on success a code is not one-time, it is a five-minute password.
        assertThat(emailService.checkVerificationCode(EMAIL, code)).isFalse();
    }

    @Test
    @DisplayName("A second send inside the cooldown is refused with 429")
    void send_insideCooldown_isRefused() throws Exception {
        sendAndReadCode();

        // Unlimited sends make a public endpoint a mail relay pointed at whoever the caller
        // names, and they reset the code often enough to matter for guessing too.
        assertThatThrownBy(() -> emailService.sendVerificationCode(EMAIL))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Repeated wrong guesses lock the address, and the lock outlives the code")
    void repeatedFailures_lockTheAddress() throws Exception {
        String code = sendAndReadCode();

        // Default max-attempts is 5. Six digits is a million possibilities only if the
        // number of guesses is bounded; unbounded, it is a formality.
        for (int i = 0; i < 5; i++) {
            assertThat(emailService.checkVerificationCode(EMAIL, "000000")).isFalse();
        }

        assertThatThrownBy(() -> emailService.checkVerificationCode(EMAIL, code))
                .as("even the correct code is refused once the address is locked")
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Guessing against an address that was never sent a code still counts")
    void guessingWithoutASend_stillCounts() {
        // Otherwise the lockout is trivially avoided: never trigger a send, guess forever.
        for (int i = 0; i < 5; i++) {
            assertThat(emailService.checkVerificationCode(EMAIL, "000000")).isFalse();
        }
        assertThatThrownBy(() -> emailService.checkVerificationCode(EMAIL, "000000"))
                .isInstanceOf(BizException.class);
    }
}
