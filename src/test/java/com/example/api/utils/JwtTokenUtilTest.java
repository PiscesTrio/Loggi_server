package com.example.api.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterization test for the JWT convention (prefix + HS256 + roles claim).
 *
 * <p>S00 left a gap: {@code AdminControllerTest} stops at the "token is empty" early return, so
 * jjwt is never invoked and the only two {@code JwtTokenUtil} paths that encode and decode a
 * token — issuing and parsing — sit outside the safety net. S01 swaps the framework underneath
 * them, so this class pins the round trip as a pre-upgrade baseline.
 */
class JwtTokenUtilTest {

    private static final String SECRET = "characterization-secret";

    @BeforeEach
    void injectSecret() {
        // APP_SECRET is static, copied from the instance field by @PostConstruct
        // (removing static is left to a later slice)
        JwtTokenUtil util = new JwtTokenUtil();
        ReflectionTestUtils.setField(util, "appSecretValue", SECRET);
        util.init();
    }

    @Test
    @DisplayName("A freshly created token round-trips back to the same subject and roles")
    void createToken_thenRead_returnsSameSubjectAndRoles() {
        String token = JwtTokenUtil.createToken(
                "alice", new String[]{"ROLE_ADMIN"}, JwtTokenUtil.EXPIRATION_TIME);

        assertThat(token).startsWith("logistics:");
        assertThat(JwtTokenUtil.checkToken(token)).isTrue();
        assertThat(JwtTokenUtil.getUsername(token)).isEqualTo("alice");
        assertThat(JwtTokenUtil.getTokenRoles(token)).containsExactly("ROLE_ADMIN");
        assertThat(JwtTokenUtil.isExpiration(token)).isFalse();
    }

    @Test
    @DisplayName("checkToken rejects anything without the logistics: prefix")
    void checkToken_withoutPrefix_isRejected() {
        String unprefixed = Jwts.builder()
                .setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() + JwtTokenUtil.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();

        assertThat(JwtTokenUtil.checkToken(unprefixed)).isFalse();
        assertThat(JwtTokenUtil.checkToken(null)).isFalse();
        assertThat(JwtTokenUtil.checkToken("")).isFalse();
        assertThat(JwtTokenUtil.checkToken("null")).isFalse();
    }

    @Test
    @DisplayName("A token signed with a different secret fails signature verification")
    void getUsername_withForeignSignature_throws() {
        String foreign = "logistics:" + Jwts.builder()
                .setSubject("mallory")
                .setExpiration(new Date(System.currentTimeMillis() + JwtTokenUtil.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, "a-completely-different-secret")
                .compact();

        assertThatThrownBy(() -> JwtTokenUtil.getUsername(foreign))
                .isInstanceOf(io.jsonwebtoken.SignatureException.class);
    }
}
