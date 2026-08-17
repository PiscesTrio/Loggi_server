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
 * <p>S00 左了一个洞：{@code AdminControllerTest} 只走到「token 为空」的提前返回，jjwt 从未被真正
 * 调用过。于是 {@code JwtTokenUtil} 里唯一会触发 jjwt 编解码的两条路径（签发与解析）在整个安全网上
 * 没有任何覆盖——而 S01 恰恰要动它们脚下的框架。这个类补上这段往返，作为升级前的行为基线。
 */
class JwtTokenUtilTest {

    private static final String SECRET = "characterization-secret";

    @BeforeEach
    void injectSecret() {
        // APP_SECRET 是 static 字段，由 @PostConstruct 从实例字段拷过去（去 static 留给后续 Slice）
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
