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
 * 为编译通过所做的最小必要适配，**语义与升级前逐行等价**。
 *
 * <p>WebSecurityConfigurerAdapter 与 authenticationManagerBean() 在新版 Spring Security 中已被
 * 物理删除，不改无法编译。这里只把「怎么注册」换成组件式写法，「注册了什么」一个字没动：
 * 同样 disable csrf、开 cors、STATELESS、挂同一个 JwtAuthorizationFilter，同样不声明任何
 * 请求级授权规则（授权全靠 @PreAuthorize）。
 *
 * <p>真正的语义现代化——lambda 细粒度授权、JWT 过滤器改 OncePerRequestFilter、去 static——
 * 属于 S02，本 Slice 刻意不做。
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)   // 取代已删除的 @EnableGlobalMethodSecurity
public class SecurityConfiguration {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JwtAuthorizationFilter 继承 BasicAuthenticationFilter，构造器需要一个 AuthenticationManager。
     * 旧代码从 authenticationManagerBean() 取，该方法已随 adapter 一同删除，改由容器暴露。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        // 开启跨域
        http.csrf(csrf -> csrf.disable())
                // 空 lambda 不是笔误：它启用 CORS 并让 Spring 去发现下面的 corsConfigurationSource Bean。
                // 漏掉这一行等价于不开 CORS，前端会撞 cors error。
                .cors(cors -> {
                })
                // 禁用 session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 添加自定义的 jwt 过滤器
                .addFilter(new JwtAuthorizationFilter(authenticationManager));
        return http.build();
    }

    /**
     * SpringSecurity有默认的跨域配置 会无法放行RequestHeader带有"Authorization"请求
     * 防止前端请求api报出cors error
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
