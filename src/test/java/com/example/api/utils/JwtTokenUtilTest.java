package com.example.api.utils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the issue/verify contract.
 *
 * <p>Every case here is written against a plain {@code new JwtTokenUtil(secret)}. That is only
 * possible because the class stopped being static: the previous version copied its secret into a
 * static field from a {@code @PostConstruct} hook, so a test had to reach in with
 * {@code ReflectionTestUtils} and hope no other test had already populated it.
 */
class JwtTokenUtilTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "a-different-secret-also-at-least-32-bytes";

    private final JwtTokenUtil util = new JwtTokenUtil(SECRET);

    @Test
    @DisplayName("A freshly issued token round-trips back to the same subject and roles")
    void createToken_thenParse_returnsSameSubjectAndRoles() {
        String token = util.createToken(
                "alice@example.com", List.of("ROLE_ADMIN"), JwtTokenUtil.EXPIRATION_TIME);

        assertThat(util.getUsername(token)).isEqualTo("alice@example.com");
        assertThat(util.getRoles(token)).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("The issued token carries no application prefix")
    void createToken_producesABareJwt() {
        String token = util.createToken("alice", List.of(), JwtTokenUtil.EXPIRATION_TIME);

        // Three base64url segments and nothing else. The old format prepended "logistics:",
        // which the filter then accepted as evidence the token was genuine.
        assertThat(token).matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    }

    @Test
    @DisplayName("A token signed with another secret fails verification")
    void parse_withForeignSignature_throws() {
        SecretKey attacker = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("mallory")
                .claim("roles", List.of("ROLE_SUPER_ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(attacker)
                .compact();

        assertThatThrownBy(() -> util.parse(forged)).isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("An expired token is rejected by parse, not by a separate check")
    void parse_expiredToken_throws() {
        // Expiry is part of verification now. Previously it was a standalone isExpiration()
        // call the caller had to remember to make.
        String expired = util.createToken("bob", List.of("ROLE_ADMIN"), -1_000);

        assertThatThrownBy(() -> util.parse(expired)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Garbage in the token position is rejected, not passed through")
    void parse_malformedToken_throws() {
        assertThatThrownBy(() -> util.parse("not-a-jwt")).isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("A secret below 256 bits is refused at construction")
    void constructor_shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtTokenUtil("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("The CHANGE_ME placeholder is refused at construction")
    void constructor_placeholderSecret_failsFast() {
        // Note this one would pass the length check if the placeholder were longer: it is
        // rejected for being *public*, which no amount of entropy in a committed default fixes.
        assertThatThrownBy(() -> new JwtTokenUtil("CHANGE_ME"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHANGE_ME");
    }

    @Test
    @DisplayName("resolveToken strips the Bearer scheme and ignores everything else")
    void resolveToken_handlesHeaderVariants() {
        assertThat(util.resolveToken("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(util.resolveToken(null)).isNull();
        assertThat(util.resolveToken("")).isNull();
        assertThat(util.resolveToken("Bearer ")).isNull();
        assertThat(util.resolveToken("Basic abc")).isNull();
        // The old transport convention is no longer recognised, by design.
        assertThat(util.resolveToken("logistics:abc.def.ghi")).isNull();
    }

    @Test
    @DisplayName("A token with no roles claim yields no authorities rather than failing")
    void getRoles_withoutRolesClaim_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String rolelessButValid = Jwts.builder()
                .subject("carol")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThatCode(() -> util.getRoles(rolelessButValid)).doesNotThrowAnyException();
        assertThat(util.getRoles(rolelessButValid)).isEmpty();
    }
}
