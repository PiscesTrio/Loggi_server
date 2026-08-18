package com.example.api.repository;

import com.example.api.model.entity.Admin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

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
    @DisplayName("The account is found by e-mail alone, and the column holds a full hash")
    void findAdminByEmail_returnsTheAccountAndItsStoredHash() {
        // Replaces a test that pinned findAdminByEmailAndPassword matching a plaintext
        // password. S00 wrote that one knowing the BCrypt slice would delete the query it
        // described; this is what took its place.
        String hash = "{bcrypt}$2a$10$Qu7Ns1ky0lClGLYVviA1Fuuz2jEf4PiE/Nv7a9Kh9Sq8F30uStOxC";
        Admin a = new Admin();
        a.setEmail("admin@logi.com");
        a.setPassword(hash);
        a.setRoles("ROLE_SUPER_ADMIN");
        adminRepository.save(a);

        Admin found = adminRepository.findAdminByEmail("admin@logi.com");
        assertThat(found).isNotNull();
        // The real assertion: the column round-trips the hash intact. At varchar(30) it
        // would come back truncated to 30 characters and every login would fail with no
        // error anywhere - the write succeeds, only the comparison quietly stops matching.
        assertThat(found.getPassword()).isEqualTo(hash);
        assertThat(hash).hasSize(68);
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
