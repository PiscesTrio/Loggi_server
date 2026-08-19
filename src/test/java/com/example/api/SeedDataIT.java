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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
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
            .withDatabaseName("loggi");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        // No ddl-auto override. It used to say "update" to match production; production is
        // Flyway plus validate since S08, so the honest way to match it is to let the real
        // configuration run. These containers start empty, so Flyway applies V1 onwards and
        // Hibernate then validates the entities against what the migrations built — which
        // makes every one of these tests a check that the two still agree.
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
    // findById does not fetch the association, and the associations are LAZY on purpose, so
    // reading one after the repository call needs a session still open. Production never
    // takes this path — the controller reads through findAll, whose entity graph loads all
    // three — and the test below is the one that guards that.
    @Transactional
    @DisplayName("A seeded distribution resolves its origin warehouse and keeps care's trailing comma")
    void seededDistribution_resolvesItsWarehouseAndHonoursTheCareTrap() {
        Optional<Distribution> dis = distributionRepository.findById("seed-dis-1");

        assertThat(dis).isPresent();
        // This assertion used to read getWid() and expect the warehouse NAME, because that
        // is what the column held — the trap this slice removed. The seed now stores an id
        // behind a real foreign key, and the name is read through the association.
        assertThat(dis.get().getWarehouse().getName()).isEqualTo("東京江東倉庫");
        // The client builds this string with a trailing comma; seeded rows must
        // have the same shape as real ones.
        assertThat(dis.get().getCare()).endsWith(",");
        assertThat(dis.get().getToLat()).isBetween(33.0, 34.0);   // Fukuoka
    }

    @Test
    @DisplayName("findAll returns orders whose associations survive the closed session")
    void findAll_loadsAssociationsEagerlyEnoughToSerialise() {
        // The production path, asserted without a transaction on purpose. The controller
        // hands these entities to Jackson after the transaction has closed, so anything the
        // entity graph failed to fetch becomes a LazyInitializationException on a live
        // request rather than a slow query. Reading each association here is what a
        // serialiser does; if this passes, serialisation cannot fail for want of a session.
        List<Distribution> all = distributionRepository.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all)
                .allSatisfy(d -> {
                    assertThat(d.getDriver().getName()).isNotBlank();
                    assertThat(d.getVehicle().getNumber()).isNotBlank();
                    assertThat(d.getWarehouse().getName()).isNotBlank();
                });
    }
}
