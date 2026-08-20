package com.example.api.service.impl;

import com.example.api.config.VerificationProperties;
import com.example.api.exception.BizException;
import com.example.api.service.EmailService;
import com.example.api.utils.RandomUtil;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * One-time codes for e-mail login.
 *
 * <p>What this replaces was not an unfinished feature, it was an unlocked door. The code was the
 * literal {@code "123456"}, {@code mailSender.send(...)} sat commented out, and the endpoint that
 * issues codes is public — so two unauthenticated requests produced a {@code ROLE_SUPER_ADMIN}
 * token for any address the caller could name. Confirmed against a running server before this
 * rewrite, not inferred from the source.
 *
 * <p>Four things had to be true at once for that to work, so all four changed:
 *
 * <ul>
 *   <li>the code is random ({@link RandomUtil}, now SecureRandom) rather than constant;
 *   <li>it is actually sent, so possession of the mailbox is what proves anything;
 *   <li>it is stored hashed and expiring, so reading the store hands out nothing live and nothing
 *       accumulates;
 *   <li>guessing and sending are both rate-limited, so six digits is a real space.
 * </ul>
 *
 * <p>Redis rather than the JPA {@code Code} table this used to write. A one-time code is cache
 * data: short-lived, single-use, write-heavy. {@code SET ... EX} expires it without the
 * application-level {@code exp} column that nothing ever cleaned up, and {@code INCR} makes the
 * attempt counter atomic — a read-then-write over JPA loses count under concurrency, and
 * concurrency is the one condition an attacker controls freely.
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String CODE_KEY = "verification:code:";
    private static final String COOLDOWN_KEY = "verification:cooldown:";
    private static final String ATTEMPTS_KEY = "verification:attempts:";
    private static final String LOCK_KEY = "verification:lock:";

    @Resource private StringRedisTemplate redis;

    @Resource private JavaMailSender mailSender;

    @Resource private VerificationProperties properties;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String email) {
        // SETNX: claim the cooldown slot, and send only if this call is the one that got it.
        // Asking "does the key exist" and then setting it is two round trips with a gap
        // between them, and that gap is exactly where a burst of concurrent requests lands.
        Boolean claimed =
                redis.opsForValue()
                        .setIfAbsent(
                                COOLDOWN_KEY + email,
                                "1",
                                Duration.ofSeconds(properties.sendCooldownSeconds()));
        if (!Boolean.TRUE.equals(claimed)) {
            throw new BizException(429, "请求过于频繁，请稍后再试");
        }

        String code = RandomUtil.next();
        long ttlSeconds = properties.codeTtlSeconds();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("验证码");
        message.setText("你的验证码为: " + code + " ，" + (ttlSeconds / 60) + " 分钟内有效。");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Release the cooldown. The caller received nothing, so making them wait a
            // minute to retry punishes them for the mail server's failure.
            redis.delete(COOLDOWN_KEY + email);
            log.error("Could not send a verification code to {}", email, e);
            throw new BizException(502, "验证码发送失败，请稍后再试");
        }

        // Only the hash is stored. Six digits is a million possibilities, so this is no
        // obstacle to anyone willing to compute; it addresses the passive case, where
        // whoever can read the store would otherwise be reading live credentials.
        redis.opsForValue().set(CODE_KEY + email, hash(code), Duration.ofSeconds(ttlSeconds));
        redis.delete(ATTEMPTS_KEY + email);
        log.info("Sent a verification code to {}", email);
    }

    @Override
    public boolean checkVerificationCode(String email, String code) {
        if (Boolean.TRUE.equals(redis.hasKey(LOCK_KEY + email))) {
            throw new BizException(429, "验证失败次数过多，请稍后再试");
        }
        if (code == null || code.isBlank()) {
            return false;
        }

        String stored = redis.opsForValue().get(CODE_KEY + email);
        if (stored == null) {
            // Expired, already used, or never issued. Still a failed attempt: otherwise an
            // attacker guesses freely as long as they never trigger a send.
            countFailure(email);
            return false;
        }

        // Constant time. A comparison that returns on the first differing character reports,
        // through how long it took, how much of the code was right.
        if (!MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8))) {
            countFailure(email);
            return false;
        }

        // Single use. Deleting on success is the whole difference between a one-time code
        // and a five-minute password.
        redis.delete(CODE_KEY + email);
        redis.delete(ATTEMPTS_KEY + email);
        return true;
    }

    /**
     * Counts a wrong guess, and locks the address once there have been too many.
     *
     * <p>INCR is atomic, so two parallel guesses cannot both read 4 and both write 5. The TTL is
     * set on the first increment only — refreshing it on every attempt would let a patient attacker
     * hold the window open indefinitely without ever reaching the limit.
     */
    private void countFailure(String email) {
        Long attempts = redis.opsForValue().increment(ATTEMPTS_KEY + email);
        if (attempts == null) {
            return;
        }
        if (attempts == 1L) {
            redis.expire(ATTEMPTS_KEY + email, Duration.ofSeconds(properties.codeTtlSeconds()));
        }
        if (attempts >= properties.maxAttempts()) {
            redis.opsForValue()
                    .set(LOCK_KEY + email, "1", Duration.ofSeconds(properties.lockSeconds()));
            redis.delete(ATTEMPTS_KEY + email);
            redis.delete(CODE_KEY + email);
            log.warn("Locked verification for {} after {} failed attempts", email, attempts);
        }
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Every JVM is required to provide SHA-256; without it the platform is broken.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
