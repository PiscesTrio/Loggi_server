package com.example.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 框架升级最直接的烟雾测试：把**完整**的 ApplicationContext 装起来。
 *
 * <p>其余测试都只覆盖切片——@WebMvcTest 只有 web 层、@DataJpaTest 只有 JPA 层、
 * 两个 Mockito 单测连 Spring 都不启动。于是「所有 Bean 能不能在新基线下一起装配成功」
 * 这件事在 S01 之前**没有任何断言看着**：AOP 切面、邮件、后台任务、安全过滤器链，
 * 任何一个在大版本跨越中坏掉都不会让构建变红。
 *
 * <p>用 RANDOM_PORT 而非默认的 MOCK：这样会**真的启动内嵌 Web 容器**，等价于 S01 验收里
 * 那条「`spring-boot:run` 能正常启动」的冒烟，而且是每次构建都跑的常驻回归，不是一次性手工确认。
 *
 * <p>是 *IT 而非 *Test：完整上下文需要真实数据源，走 failsafe + Testcontainers。
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationContextIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update"); // match production
    }

    @Autowired ApplicationContext context;

    @Test
    @DisplayName("The whole application context loads on the upgraded baseline")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("The security filter chain is still assembled by the rewritten configuration")
    void securityFilterChainIsRegistered() {
        // S01 把 WebSecurityConfigurerAdapter 改写成了 SecurityFilterChain Bean。
        // 只断言「链装起来了」——具体规则是 S02 的事，这里不去钉它的内容。
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
    }
}
