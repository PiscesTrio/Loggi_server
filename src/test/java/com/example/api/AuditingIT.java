package com.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.model.entity.Commodity;
import com.example.api.repository.CommodityRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Timestamps are written by the database layer, not by whoever remembered to call a setter.
 *
 * <p>This exists because the responsibility moved. Every service used to set {@code createAt}
 * itself, so a unit test with a mocked repository could see it happen; auditing runs inside a real
 * persistence context, so those assertions had nothing left to observe and were removed from the
 * service tests rather than weakened. The check belongs here now — against a real database, which
 * is also the only place the column type is exercised.
 */
@Testcontainers
@SpringBootTest
class AuditingIT {

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

    @Autowired CommodityRepository commodityRepository;

    @Test
    @DisplayName("Both timestamps are set on insert, without the service touching them")
    void insertFillsBothTimestamps() {
        Commodity saved = commodityRepository.saveAndFlush(commodity("監査検証用商品A"));

        assertThat(saved.getCreateAt()).isNotNull();
        assertThat(saved.getUpdateAt()).isNotNull();
    }

    @Test
    @DisplayName("An update moves updateAt and leaves createAt alone")
    void updateMovesOnlyTheModificationTime() {
        Commodity saved = commodityRepository.saveAndFlush(commodity("監査検証用商品B"));
        LocalDateTime created = saved.getCreateAt();
        LocalDateTime firstUpdate = saved.getUpdateAt();

        saved.setCount(99);
        Commodity updated = commodityRepository.saveAndFlush(saved);

        // createAt is mapped updatable = false, so this holds even if a caller tries to change it.
        assertThat(updated.getCreateAt()).isEqualTo(created);
        assertThat(updated.getUpdateAt()).isAfterOrEqualTo(firstUpdate);
    }

    @Test
    @DisplayName("A timestamp supplied by the caller is overwritten, not trusted")
    void callerSuppliedTimestampIsIgnored() {
        // The old String columns took whatever arrived. These describe what the database did;
        // accepting a client's version of that would make them worthless as an audit trail.
        Commodity incoming = commodity("監査検証用商品C");
        incoming.setCreateAt(LocalDateTime.of(1999, 1, 1, 0, 0));

        Commodity saved = commodityRepository.saveAndFlush(incoming);

        assertThat(saved.getCreateAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    private static Commodity commodity(String name) {
        Commodity c = new Commodity();
        c.setName(name);
        c.setPrice(new java.math.BigDecimal("1000.00"));
        c.setCount(1);
        return c;
    }

    @Test
    @DisplayName("The recorded time is the moment the application saw, not one shifted by a zone")
    void timestampsAreNotShiftedByTheDriver() {
        // Regression. LocalDateTime and MySQL's `datetime` both describe a wall clock with no
        // zone attached, so the driver must not convert between them — and it was. The URL
        // declared serverTimezone=Asia/Shanghai while the server runs on UTC, so a value
        // written as 09:00 read back as 10:00, and rows the application wrote were stored an
        // hour before the moment they describe. The round trip was self-consistent, which is
        // why it stayed invisible until the string columns became real datetimes.
        //
        // This container runs on UTC and the build does not, so any reintroduced conversion
        // shows up here as a difference of whole hours.
        LocalDateTime before = LocalDateTime.now();
        Commodity saved = commodityRepository.saveAndFlush(commodity("時刻検証用商品"));
        LocalDateTime after = LocalDateTime.now();

        assertThat(saved.getCreateAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }
}
