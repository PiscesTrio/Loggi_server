package com.example.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Minimal adaptation to make the code compile again; <b>semantics are line-for-line equivalent to
 * the pre-upgrade config</b>.
 *
 * <p>WebSecurityConfigurerAdapter and authenticationManagerBean() were removed outright in current
 * Spring Security, so this could not compile untouched. Only the <b>how</b> of registration changed
 * — component style instead of the adapter; the <b>what</b> is identical: csrf disabled, cors on,
 * STATELESS, the same JwtAuthorizationFilter, no request-level rules (authorization still rests
 * entirely on {@code @PreAuthorize}).
 *
 * <p>The real semantic modernization — lambda fine-grained authorization, moving the JWT filter to
 * OncePerRequestFilter, dropping static — belongs to S02, deliberately out of scope for this slice.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)   // replaces the removed @EnableGlobalMethodSecurity
public class SecurityConfiguration {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JwtAuthorizationFilter extends BasicAuthenticationFilter, whose constructor requires an
     * AuthenticationManager. The old code pulled it from authenticationManagerBean(), which was
     * removed along with the adapter, so the container exposes it as a bean instead.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        // enable CORS
        http.csrf(csrf -> csrf.disable())
                // The empty lambda is not a typo: it turns CORS on and lets Spring discover the
                // corsConfigurationSource bean below. Omitting this line means no CORS at all and
                // the frontend hits a cors error.
                .cors(cors -> {
                })
                // disable sessions
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // register the custom jwt filter
                .addFilter(new JwtAuthorizationFilter(authenticationManager));
        return http.build();
    }

    /**
     * Spring Security's default CORS configuration blocks requests whose RequestHeader carries
     * "Authorization"; this bean keeps the frontend from getting a cors error on api calls.
     *
     * @return *
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedHeader("DELETE");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}
