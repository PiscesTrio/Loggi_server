package com.example.api.security;

import com.example.api.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Component-based security configuration.
 *
 * <p>S01 turned the old {@code WebSecurityConfigurerAdapter} into a {@link SecurityFilterChain}
 * bean as the minimum needed to compile. This finishes the job: the JWT filter no longer borrows
 * {@code BasicAuthenticationFilter}, so the {@code AuthenticationManager} that existed only to
 * satisfy its constructor is gone. A stateless JWT application never authenticates a
 * username/password pair at the filter layer — it only reads tokens it issued earlier — so that
 * manager had nothing to do.
 *
 * <p>Request-level authorization is a default-deny matrix: a short list of endpoints that
 * must work before a token exists, and {@code authenticated()} for everything else.
 * {@code @PreAuthorize} still narrows individual methods by role, but it is no longer the
 * only thing between an anonymous caller and the data.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /** Comma-separated origin patterns; see {@code cors.allowed-origins}. */
    private final List<String> allowedOrigins;

    public SecurityConfiguration(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Password hashing for the whole application.
     *
     * <p>A BCryptPasswordEncoder bean stood here since the project began and nothing
     * ever injected it, while passwords were stored and compared in plain text. It is
     * now the encoder the service actually uses.
     *
     * <p>Delegating rather than plain BCrypt: it writes an algorithm prefix into the
     * hash and reads that prefix back when verifying, so the day BCrypt needs replacing,
     * existing hashes keep working while new ones use the new algorithm. Plain BCrypt
     * would make that a migration.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenUtil jwtTokenUtil)
            throws Exception {
        return http
                // No cookies and no session, so there is no CSRF vector to protect.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight carries no credentials and must never be challenged.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Everything below is the complete public surface. Each entry is
                        // here because it must work before a token exists:
                        //   login/*   — where tokens come from
                        //   hasInit   — asked on a fresh install, which has no account
                        //   init      — creates the first account; guarded by hasInit,
                        //               not by authentication, since there is nobody to
                        //               authenticate as yet
                        //   verification-code — the e-mail login path's first step
                        .requestMatchers(HttpMethod.POST, "/api/admin/login/password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/login/email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/init").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/hasInit").permitAll()
                        // Was GET /api/admin/sendEmail. A request that sends mail and writes
                        // server state is not a GET: it is retried by proxies, prefetched by
                        // browsers, and logged with its parameters. Rate limiting lives in the
                        // service, because a matcher cannot count.
                        .requestMatchers(HttpMethod.POST, "/api/admin/verification-code").permitAll()
                        // Health, and only health. An orchestrator probes it before the
                        // application has any way to authenticate, so it cannot require a
                        // token; the rest of /actuator can and does. show-details is off in
                        // configuration, so this answers UP or DOWN and nothing about what
                        // is broken.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        // The API documentation. Public deliberately: every endpoint it
                        // describes is itself authenticated, so hiding the description is
                        // obscurity rather than a control - and for a portfolio, being able
                        // to open the API and read it is the point. If this were a real
                        // deployment the trade-off would go the other way, because a
                        // published surface is a shorter path to whatever is weakest in it.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // Default deny. Until now this line read permitAll(), so URL-level
                        // authorization did not exist and the only thing in front of the
                        // data was whatever @PreAuthorize a controller happened to carry —
                        // seven of the thirteen carry none.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
                .addFilterBefore(new JwtAuthorizationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Answers 401 when a request arrives without credentials.
     *
     * <p>Spring Security's default for a denied anonymous request is 403, because an
     * anonymous authentication technically exists and is simply not permitted. For an API
     * that is the wrong answer: 403 tells a client "you are known and not allowed", which
     * sends it looking for a permissions problem, when the real answer is "you did not
     * present a token". It also disagreed with JwtAuthorizationFilter, which already
     * answers 401 for a token that fails to verify — the same situation from the client's
     * point of view, reported two different ways.
     *
     * <p>Same envelope as the filter's, so a client parses one shape whichever layer
     * refused it.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":401,\"status\":false,\"msg\":\"Authentication required\",\"data\":null}");
        };
    }

    /**
     * CORS for a browser-based client.
     *
     * <p>Uses {@code allowedOriginPatterns} rather than the previous {@code addAllowedOrigin("*")}:
     * a wildcard origin is illegal together with credentials and browsers reject that pairing. The
     * old configuration also listed {@code DELETE} as an allowed *header* — that is a method, so the
     * entry allowed nothing while giving the impression headers were being restricted.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        // The token travels in a header, not a cookie, so no credentialed request is ever made.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
