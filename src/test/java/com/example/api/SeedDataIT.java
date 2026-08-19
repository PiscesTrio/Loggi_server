package com.example.api;

import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Warehouse;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that {@code data.sql} actually ran.
 *
 * <p>Without this, a green build says nothing about the seed: the script is executed
 * by ScriptUtils at DEBUG level, so a run that never happened and a run that
 * succeeded look identical in the log. "No error" is not evidence of execution —
 * only reading the rows back is.
 *
 * <p>It also pins the two denormalisation traps the seed has to honour, because both
 * are invisible until something renders them: {@code distribution.wid} holds the
 * warehouse NAME (while {@code inventory.wid} holds the id), and {@code care} is
 * comma-joined WITH a trailing comma.
 */
@Testcontainers
@SpringBootTest
class SeedDataIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        // The context will not start without one — see ApplicationContextIT.
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
    }

    @Autowired WarehouseRepository warehouseRepository;
    @Autowired DistributionRepository distributionRepository;

    @Test
    @DisplayName("The seeded warehouses exist and carry Japanese WGS-84 coordinates")
    void seededWarehouses_haveJapaneseCoordinates() {
        Optional<Warehouse> tokyo = warehouseRepository.findById("seed-wh-tokyo");

        assertThat(tokyo).isPresent();
        assertThat(tokyo.get().getName()).isEqualTo("東京江東倉庫");
        // A band, not a point: the seed addresses are fictional and their coordinates
        // may be nudged again, but they must stay in Japan. Narrow enough to fail if
        // they ever revert to the previous Chinese ones, which is the regression that
        // matters.
        assertThat(tokyo.get().getLat()).isBetween(35.0, 36.0);
        assertThat(tokyo.get().getLng()).isBetween(139.0, 140.0);

        assertThat(warehouseRepository.findAll())
                .extracting(Warehouse::getName)
                .contains("東京江東倉庫", "大阪此花倉庫", "名古屋港倉庫");
    }

    @Test
    @DisplayName("A seeded distribution keeps the warehouse NAME in wid and a trailing comma in care")
    void seededDistribution_honoursTheDenormalisationTraps() {
        Optional<Distribution> dis = distributionRepository.findById("seed-dis-1");

        assertThat(dis).isPresent();
        // wid is the warehouse name here — an id would render as an id in the UI.
        assertThat(dis.get().getWid()).isEqualTo("東京江東倉庫");
        // The client builds this string with a trailing comma; seeded rows must
        // have the same shape as real ones.
        assertThat(dis.get().getCare()).endsWith(",");
        assertThat(dis.get().getToLat()).isBetween(33.0, 34.0);   // Fukuoka
    }
}
