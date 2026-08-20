package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V9 converts rows that already exist, and nothing else in this suite ever gives it one to convert.
 *
 * <p>Every other container here starts empty, so Flyway runs V9 against zero drivers, zero vehicles
 * and zero orders: the CREATE TABLE and the ALTERs are exercised and the eight INSERT..SELECTs
 * match nothing. A green suite would therefore say nothing at all about whether a comma-joined care
 * list becomes the right rows — which is the only part of this migration that can silently destroy
 * data, and the one part no later script can undo.
 *
 * <p>This is the same hole a used database found the hard way once before: data.sql deleted its
 * seed rows on every boot, and the application stopped starting the moment anyone recorded a stock
 * movement, because CI always started from empty and never booted twice. "A fresh clone works" and
 * "an existing database survives" are different claims, and only one of them was being tested.
 *
 * <p>So: migrate to V8, write rows in the old shape by hand, then migrate to V9 and look.
 */
@Testcontainers
class WireValueMigrationIT {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateThroughV9() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(MYSQL.getJdbcUrl());
        ds.setUsername(MYSQL.getUsername());
        ds.setPassword(MYSQL.getPassword());
        jdbc = new JdbcTemplate(ds);

        migrateTo(ds, "8");
        seedLegacyRows();
        migrateTo(ds, "9");
    }

    private static void migrateTo(DataSource ds, String version) {
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    /** The database as it looked before V9: Chinese in three places, one of them comma-joined. */
    private static void seedLegacyRows() {
        jdbc.update(
                "INSERT INTO driver (id, name, gender, driving) VALUES ('d1','tanaka','男性',0),"
                        + " ('d2','sasaki','女性',0), ('d3','kobayashi','その他',0)");
        jdbc.update("INSERT INTO employee (id, name, gender) VALUES ('e1','yamada','女性')");
        jdbc.update(
                "INSERT INTO vehicle (id, number, type, driving) VALUES ('v1','p1','货车',0),"
                        + " ('v2','p2','卡车',0), ('v3','p3','重卡',0),"
                        + " ('v4','p4','三輪車',0)");

        // The five care shapes that exist in the wild. The trailing comma is what the client
        // appends; the others are what happens when it appends it to nothing, or when a row
        // predates that behaviour.
        insertOrder("o-two", "易碎,防潮,");
        insertOrder("o-six", "冷藏,防高温,禁止翻滚," + "禁止堆码,防晒,易燃,");
        insertOrder("o-no-trailing-comma", "易碎");
        insertOrder("o-empty", "");
        insertOrder("o-null", null);
    }

    private static void insertOrder(String id, String care) {
        jdbc.update(
                "INSERT INTO distribution (id, care, urgent, from_lat, from_lng, to_lat, to_lng)"
                        + " VALUES (?, ?, 0, 35.6, 139.8, 34.6, 135.5)",
                id,
                care);
    }

    @Test
    @DisplayName("Gender labels become identifiers, and an unrecognised one becomes null")
    void genderConverted() {
        assertThat(genderOf("driver", "d1")).isEqualTo("MALE");
        assertThat(genderOf("driver", "d2")).isEqualTo("FEMALE");
        // Not guessed at. There is no constant meaning "unknown", and writing MALE here would be
        // a fabrication no later reader could tell from a real one.
        assertThat(genderOf("driver", "d3")).isNull();
        assertThat(genderOf("employee", "e1")).isEqualTo("FEMALE");
    }

    @Test
    @DisplayName("Vehicle types become identifiers, and an unrecognised one becomes null")
    void vehicleTypeConverted() {
        assertThat(typeOf("v1")).isEqualTo("LIGHT_TRUCK");
        assertThat(typeOf("v2")).isEqualTo("TRUCK");
        assertThat(typeOf("v3")).isEqualTo("HEAVY_TRUCK");
        assertThat(typeOf("v4")).isNull();
    }

    @Test
    @DisplayName("A comma-joined care list becomes one row per tag")
    void careSplitIntoRows() {
        assertThat(tagsOf("o-two")).containsExactly("FRAGILE", "KEEP_DRY");
        assertThat(tagsOf("o-six"))
                .containsExactlyInAnyOrder(
                        "REFRIGERATE",
                        "PROTECT_FROM_HEAT",
                        "DO_NOT_ROLL",
                        "DO_NOT_STACK",
                        "KEEP_AWAY_FROM_SUNLIGHT",
                        "FLAMMABLE");
        assertThat(tagsOf("o-no-trailing-comma")).containsExactly("FRAGILE");
    }

    @Test
    @DisplayName("No tags is no rows, whether it was written as empty or as null")
    void emptyCareBecomesNoRows() {
        // Three spellings of "nothing" collapsing into one is half the reason for the change.
        assertThat(tagsOf("o-empty")).isEmpty();
        assertThat(tagsOf("o-null")).isEmpty();
    }

    @Test
    @DisplayName("Nine tags in, nine rows out - no tag matched an order it was not on")
    void noTagLeakedAcrossOrders() {
        // The assertion the per-order ones cannot make. Each INSERT..SELECT runs against every
        // order, so a migration matching with LIKE instead of FIND_IN_SET could pass all three
        // tests above and still write rows onto orders that never carried the tag. 2 + 6 + 1 = 9.
        Integer total =
                jdbc.queryForObject("SELECT COUNT(*) FROM distribution_care", Integer.class);
        assertThat(total).isEqualTo(9);
    }

    @Test
    @DisplayName("The care column is gone, so nothing can write the old shape again")
    void careColumnDropped() {
        List<Map<String, Object>> columns =
                jdbc.queryForList(
                        "SELECT column_name FROM information_schema.columns"
                                + " WHERE table_schema = DATABASE() AND table_name = 'distribution'");
        assertThat(columns.stream().map(c -> String.valueOf(c.get("COLUMN_NAME"))))
                .doesNotContain("care");
    }

    private static String genderOf(String table, String id) {
        return jdbc.queryForObject(
                "SELECT gender FROM `" + table + "` WHERE id = ?", String.class, id);
    }

    private static String typeOf(String id) {
        return jdbc.queryForObject("SELECT type FROM vehicle WHERE id = ?", String.class, id);
    }

    private static List<String> tagsOf(String distributionId) {
        return jdbc.queryForList(
                "SELECT tag FROM distribution_care WHERE distribution_id = ? ORDER BY tag",
                String.class,
                distributionId);
    }
}
