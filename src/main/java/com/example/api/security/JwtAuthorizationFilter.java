package com.example.api.security;

import com.example.api.utils.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns a verified JWT into the request's authentication.
 *
 * <p>Extends {@link OncePerRequestFilter} rather than {@code BasicAuthenticationFilter}: the latter
 * means "parse HTTP Basic credentials" and was only being borrowed for its filter plumbing, at the
 * price of having to feed it an {@code AuthenticationManager} that was never used.
 *
 * <p>Three outcomes, and only three:
 *
 * <ul>
 *   <li>no {@code Bearer} token — continue unauthenticated, and let the authorization layer decide
 *       whether that is acceptable for the endpoint;
 *   <li>a token that verifies — populate the security context and continue;
 *   <li>a token that does not verify, for any reason — <strong>401</strong>.
 * </ul>
 *
 * <p>That last case is the behaviour change. Previously a forged or malformed token escaped as an
 * uncaught exception (HTTP 500, leaking a stack trace) while an expired one produced a 200 whose
 * body claimed 403 — neither of which a client can act on.
 */
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String token = jwtTokenUtil.resolveToken(request.getHeader(JwtTokenUtil.TOKEN_HEADER));

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtTokenUtil.getUsername(token);
            List<SimpleGrantedAuthority> authorities =
                    jwtTokenUtil.getRoles(token).stream().map(SimpleGrantedAuthority::new).toList();
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(username, null, authorities));
        } catch (JwtException | IllegalArgumentException e) {
            // Covers expired, forged, malformed and unsupported tokens alike. The reason is
            // logged but deliberately not returned: telling a caller *why* their token was
            // rejected is free reconnaissance.
            SecurityContextHolder.clearContext();
            log.debug("Rejecting request: {}", e.getClass().getSimpleName());
            writeUnauthorized(response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Answers 401 in the same envelope every other endpoint uses.
     *
     * <p>Not {@code sendError}: that hands the response to the container's error page, which
     * answers in a different shape — and one where {@code status} is the integer 401 while the
     * application envelope's {@code status} is a boolean. One field name, two types, decided by
     * which layer produced the response, is a trap for any client that reads the body.
     *
     * <p>The literal is written by hand rather than serialised because it is a constant: no
     * caller-supplied value is interpolated, so there is nothing here to escape.
     */
    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
                .write(
                        "{\"code\":401,\"status\":false,\"msg\":\"Invalid or expired token\",\"data\":null}");
    }
}
