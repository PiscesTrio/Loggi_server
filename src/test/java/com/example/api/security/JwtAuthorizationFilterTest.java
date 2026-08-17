package com.example.api.security;

import com.example.api.repository.AdminRepository;
import com.example.api.service.AdminService;
import com.example.api.service.LoginLogService;
import com.example.api.utils.JwtTokenUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the filter does to a request, end to end through the real security chain.
 *
 * <p>The interesting assertions are the status codes. Before this slice a token that failed
 * verification escaped the filter as an uncaught exception — HTTP 500 with a stack trace — and an
 * expired one produced HTTP 200 whose body claimed 403. Neither is something a client can branch
 * on, and neither is visible from a unit test of the parsing code alone: it takes a request
 * travelling the whole chain to observe the status that actually reaches the caller.
 */
@WebMvcTest(com.example.api.controller.AdminController.class)
@Import({SecurityConfiguration.class, JwtAuthorizationFilterTest.RealTokenUtil.class})
class JwtAuthorizationFilterTest {

    private static final String SECRET = "filter-test-secret-at-least-32-bytes-long";
    private static final String ATTACKER_SECRET = "attacker-secret-also-at-least-32-bytes!!";

    /** A permitAll endpoint — no {@code @PreAuthorize} on {@code hasInit()}. */
    private static final String OPEN = "/api/admin/hasInit";
    /** Guarded by {@code @PreAuthorize} only; there is no URL-level rule. */
    private static final String GUARDED = "/api/admin";

    @TestConfiguration
    static class RealTokenUtil {
        /**
         * A real one, not a mock. Mocking the thing under test here would mean asserting that
         * the filter calls a method, when the question is what a genuine signature check does
         * to the response.
         */
        @Bean
        JwtTokenUtil jwtTokenUtil() {
            return new JwtTokenUtil(SECRET);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenUtil jwtTokenUtil;

    @MockitoBean AdminService adminService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean LoginLogService loginLogService;

    @Test
    @DisplayName("No Authorization header: the request continues and reaches an open endpoint")
    void noHeader_reachesOpenEndpoint() throws Exception {
        mockMvc.perform(get(OPEN)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A header using another scheme is ignored, not treated as a failure")
    void otherScheme_isIgnored() throws Exception {
        mockMvc.perform(get(OPEN).header("Authorization", "Basic YWxpY2U6cA=="))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A token signed with another secret gets 401, not 500")
    void forgedSignature_returns401() throws Exception {
        SecretKey attacker = Keys.hmacShaKeyFor(ATTACKER_SECRET.getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("mallory")
                .claim("roles", List.of("ROLE_SUPER_ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(attacker)
                .compact();

        mockMvc.perform(get(OPEN).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                // Same envelope as every other endpoint, so a client parses one shape.
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @DisplayName("An expired token gets 401, not 200 with 403 in the body")
    void expiredToken_returns401() throws Exception {
        String expired = jwtTokenUtil.createToken("bob", List.of("ROLE_ADMIN"), -1_000);

        mockMvc.perform(get(OPEN).header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A structurally invalid token gets 401, not 500")
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(get(OPEN).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The old logistics: format is no longer accepted as a scheme")
    void legacyPrefixedToken_isNotTreatedAsBearer() throws Exception {
        String valid = jwtTokenUtil.createToken("alice", List.of("ROLE_ADMIN"),
                JwtTokenUtil.EXPIRATION_TIME);

        // Not a Bearer header, so it is ignored entirely: no authentication is established.
        // Every token issued before this slice stops working, which is the cost of removing a
        // transport prefix that was standing in for a signature check.
        mockMvc.perform(get(OPEN).header("Authorization", "logistics:" + valid))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A valid token grants the authorities in its roles claim")
    void validToken_grantsTheRolesItCarries() throws Exception {
        String token = jwtTokenUtil.createToken("root@example.com",
                List.of("ROLE_SUPER_ADMIN"), JwtTokenUtil.EXPIRATION_TIME);

        // The whole chain in one assertion: the token verifies, ROLE_SUPER_ADMIN is installed as
        // an authority, and @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')") admits
        // it. Contrast findAll_withoutAuth in AdminControllerTest, where the same endpoint answers
        // with code=403 in the body.
        //
        // Note this contradicts pitfall #5 of the S02 plan, which claimed hasAnyRole would prepend
        // a second ROLE_ and look for ROLE_ROLE_SUPER_ADMIN. It does not: getRoleWithDefaultPrefix
        // returns the name unchanged when it already starts with the prefix. The plan's follow-up
        // item to "fix the double prefix" is therefore a fix for a defect that does not exist —
        // and this assertion is what makes that checkable rather than arguable.
        when(adminService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(GUARDED).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value(true));
    }
}
