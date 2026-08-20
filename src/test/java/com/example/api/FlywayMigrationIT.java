package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Vehicle;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.VehicleRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The schema is built by migrations, and the entities agree with what they built.
 *
 * <p>Both halves matter and neither is visible from a passing unit test. Against an empty container
 * Flyway applies V1 and V2, then Hibernate — configured to {@code validate} — compares every entity
 * mapping to the result and refuses to start the context if any table, column or type is missing.
 * So the context starting at all is the first assertion here; the tests below pin the parts of the
 * outcome that a context can start without.
 *
 * <p>Why this could not be checked before: {@code ddl-auto: update} made the database a function of
 * the entities, so entities and schema could not disagree — they also could not be reviewed,
 * ordered, or rolled back, and anything `update` cannot express (every constraint and index below)
 * simply never existed.
 */
@Testcontainers
@SpringBootTest
class FlywayMigrationIT {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired CommodityRepository commodityRepository;
    @Autowired VehicleRepository vehicleRepository;

    @Test
    @DisplayName("Flyway applied every migration and recorded each as successful")
    void everyMigrationApplied() {
        List<String> versions =
                jdbc.queryForList(
                        "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank",
                        String.class);

        // Named explicitly rather than counted: a count passes while the wrong scripts run.
        // This list has to be extended by hand for every new migration, which is the point —
        // adding a script should be a decision someone made, not something that slid in.
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    @DisplayName("The baseline carries no table whose entity was deleted")
    void baselineHasNoOrphanTables() {
        List<String> tables =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                        String.class);

        // The whole reason V1 was generated from the entities rather than dumped from a
        // long-lived database: `update` never dropped anything, so a dump would have carried
        // these three forward for good.
        assertThat(tables)
                .doesNotContain("user", "company", "distribution_status")
                .contains("distribution_track");
    }

    @Test
    @DisplayName("Every index V2 declares exists on the table it names")
    void declaredIndexesExist() {
        assertThat(indexesOn("admin")).contains("uk_admin_email");
        assertThat(indexesOn("commodity")).contains("uk_commodity_name");
        assertThat(indexesOn("vehicle")).contains("uk_vehicle_number");
        assertThat(indexesOn("inventory"))
                .contains("uk_inventory_warehouse_commodity", "idx_inventory_commodity_id");
        assertThat(indexesOn("inventory_record"))
                .contains("idx_inventory_record_warehouse_id", "idx_inventory_record_commodity_id");
        assertThat(indexesOn("distribution_track"))
                .contains("idx_distribution_track_distribution_id");
    }

    @Test
    @DisplayName("A duplicate commodity name is refused by the database, not by the service")
    void duplicateCommodityNameIsRejected() {
        // CommodityRepository.findByName returns a single Commodity, so the code has always
        // assumed this. Until V2 nothing enforced it and the assumption was simply false.
        commodityRepository.saveAndFlush(commodityNamed("重複検証用商品"));

        assertThatThrownBy(() -> commodityRepository.saveAndFlush(commodityNamed("重複検証用商品")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A duplicate number plate is refused")
    void duplicateVehicleNumberIsRejected() {
        vehicleRepository.saveAndFlush(vehicleNumbered("品川800め99-99"));

        assertThatThrownBy(() -> vehicleRepository.saveAndFlush(vehicleNumbered("品川800め99-99")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private List<String> indexesOn(String table) {
        return jdbc.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                String.class,
                table);
    }

    private static Commodity commodityNamed(String name) {
        Commodity c = new Commodity();
        c.setName(name);
        c.setPrice(new java.math.BigDecimal("1000.00"));
        c.setCount(1);
        return c;
    }

    private static Vehicle vehicleNumbered(String number) {
        Vehicle v = new Vehicle();
        v.setNumber(number);
        v.setType("货车");
        return v;
    }
}
