package com.example.api.repository;

import com.example.api.model.entity.Admin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration tests (*IT, run by failsafe in the verify phase) that pin the derived-query
 * behavior against a real mysql:8.0 via Testcontainers.
 *
 * @AutoConfigureTestDatabase(replace = NONE) is the key: it stops @DataJpaTest from
 * swapping in an in-memory H2, otherwise MySQL dialect specifics (Admin's
 * columnDefinition="varchar(30) default 'LTD' not null") would be masked.
 *
 * Requires Docker (CI's ubuntu-latest has it). Without Docker, run `mvn test` to skip this class.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class AdminRepositoryIT {

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

    @Autowired AdminRepository adminRepository;

    @Test
    @DisplayName("findAdminByEmailAndPassword matches on the plaintext password (pins derived-query behavior)")
    void findAdminByEmailAndPassword_matchesPlaintext() {
        Admin a = new Admin();
        a.setEmail("admin@logi.com");
        a.setPassword("plain123");      // plaintext storage, pinned as-is
        a.setRoles("ROLE_SUPER_ADMIN");
        adminRepository.save(a);

        Admin found = adminRepository.findAdminByEmailAndPassword("admin@logi.com", "plain123");
        assertThat(found).isNotNull();
        assertThat(adminRepository.findAdminByEmailAndPassword("admin@logi.com", "wrong")).isNull();
    }

    @Test
    @DisplayName("existsAdminByRoles finds a super admin")
    void existsAdminByRoles_findsSuperAdmin() {
        Admin a = new Admin();
        a.setEmail("super@logi.com");
        a.setPassword("p");
        a.setRoles("ROLE_SUPER_ADMIN");
        adminRepository.save(a);
        assertThat(adminRepository.existsAdminByRoles("ROLE_SUPER_ADMIN")).isTrue();
    }
}
