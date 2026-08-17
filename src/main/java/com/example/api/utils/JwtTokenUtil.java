package com.example.api.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Issues and verifies the application's JWTs.
 *
 * <p>Every method is an instance method and the signing key arrives through the
 * constructor. The previous version kept the secret in a {@code static} field populated
 * by a {@code @PostConstruct} callback, which meant the class could not be constructed
 * in a test without booting Spring, and any code path running before that callback
 * signed with {@code null}.
 *
 * <p>Verification is now a single explicit step: {@code parser().verifyWith(key).build()
 * .parseSignedClaims(token)} either returns verified claims or throws. Previously
 * verification happened as a side effect of reading a claim, and the caller decided
 * whether a token was trustworthy by checking that it began with a fixed prefix — a
 * transport convention standing in for a cryptographic fact.
 */
@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    /** HS256 needs at least 256 bits of key material; jjwt refuses anything shorter. */
    private static final int MIN_SECRET_BYTES = 32;

    public static final String TOKEN_HEADER = "Authorization";
    /** Standard scheme. The token itself no longer carries an application prefix. */
    public static final String BEARER_PREFIX = "Bearer ";

    public static final long REMEMBER_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7;
    public static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    private static final String ROLE_CLAIMS = "roles";

    private final SecretKey key;

    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank() || "CHANGE_ME".equals(secret)) {
            throw new IllegalStateException(
                    "jwt.secret is unset or still the CHANGE_ME placeholder. Refusing to start: "
                            + "a publicly known secret lets anyone mint an administrator token.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256, got "
                            + bytes.length + ". Refusing to start.");
        }
        try {
            this.key = Keys.hmacShaKeyFor(bytes);
        } catch (WeakKeyException e) {
            // Belt and braces: the length check above should already have caught this.
            throw new IllegalStateException("jwt.secret is too weak for HS256", e);
        }
    }

    /** Issues a signed token. The caller decides the lifetime. */
    public String createToken(String username, List<String> roles, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .claims(Map.of(ROLE_CLAIMS, roles))
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature and expiry, returning the claims.
     *
     * <p>Throws on any problem — expired, forged, malformed. Callers translate that into
     * a 401 rather than swallowing it: the old code caught only {@code ExpiredJwtException}
     * and let everything else escape the filter as a 500.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /** Roles live as a JSON array under the {@code roles} claim. */
    public List<String> getRoles(String token) {
        Object claim = parse(token).get(ROLE_CLAIMS);
        if (claim instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        log.warn("Token carries no usable roles claim");
        return List.of();
    }

    /**
     * Strips the {@code Bearer } scheme from an Authorization header value.
     *
     * @return the raw token, or {@code null} when the header is absent or uses another
     *         scheme. A non-null result says nothing about validity — only {@link #parse}
     *         decides that.
     */
    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
