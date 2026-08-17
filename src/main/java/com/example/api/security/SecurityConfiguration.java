package com.example.api.security;

import com.example.api.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
 * <p>Request-level authorization stays {@code permitAll}: every endpoint remains guarded by
 * {@code @PreAuthorize} exactly as before. Building a real authorization matrix is its own slice;
 * doing it here would hide a behaviour change inside a refactor.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /** Comma-separated origin patterns; see {@code cors.allowed-origins}. */
    private final List<String> allowedOrigins;

    public SecurityConfiguration(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
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
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtAuthorizationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
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
