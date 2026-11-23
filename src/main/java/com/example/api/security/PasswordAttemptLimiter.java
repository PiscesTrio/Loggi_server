package com.example.api.security;

import com.example.api.config.VerificationProperties;
import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Bounds how many wrong passwords an address can be tried with.
 *
 * <p>The one-time code path has had a ceiling and a lockout since it was rebuilt; the password
 * path, one method away, had neither, so it could be guessed against as fast as the network
 * allowed. A six-digit code is safe only because its guesses are counted, and a password is no
 * different.
 *
 * <p>The same ceiling and lock length as the code path ({@code app.verification.max-attempts} and
 * {@code lock-seconds}), counted separately: a wrong code should not use up password attempts, or
 * the other way round. Keyed on the address, not the client IP - {@code X-Forwarded-For} is
 * whatever the caller writes. The cost of keying on the address is that a stranger can lock an
 * account out for the lock period; that is the usual trade, and it is bounded.
 */
@Slf4j
@Component
public class PasswordAttemptLimiter {

    private static final String ATTEMPTS_KEY = "login:attempts:";
    private static final String LOCK_KEY = "login:lock:";

    @Resource private StringRedisTemplate redis;

    @Resource private VerificationProperties properties;

    /** Refuses outright, before the password is even looked at, while the address is locked. */
    public void checkNotLocked(String email) {
        if (Boolean.TRUE.equals(redis.hasKey(LOCK_KEY + key(email)))) {
            throw new BizException(
                    ErrorCode.LOGIN_ATTEMPTS_EXHAUSTED, "too many failed sign-in attempts");
        }
    }

    /**
     * Counts a wrong password, and locks the address once there have been too many.
     *
     * <p>INCR is atomic, so parallel guesses cannot share a count. The window starts at the first
     * failure and is not refreshed, so a slow attacker cannot keep it open forever.
     */
    public void recordFailure(String email) {
        String k = key(email);
        Long attempts = redis.opsForValue().increment(ATTEMPTS_KEY + k);
        if (attempts == null) {
            return;
        }
        if (attempts == 1L) {
            redis.expire(ATTEMPTS_KEY + k, Duration.ofSeconds(properties.lockSeconds()));
        }
        if (attempts >= properties.maxAttempts()) {
            redis.opsForValue()
                    .set(LOCK_KEY + k, "1", Duration.ofSeconds(properties.lockSeconds()));
            redis.delete(ATTEMPTS_KEY + k);
            log.warn("Locked password sign-in for {} after {} failed attempts", email, attempts);
        }
    }

    /** A correct password clears the count, so earlier typos do not carry over. */
    public void recordSuccess(String email) {
        redis.delete(ATTEMPTS_KEY + key(email));
    }

    /** Case-folded, so Admin@ and admin@ share one counter rather than doubling the budget. */
    private static String key(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
