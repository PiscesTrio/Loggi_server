package com.example.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.DistributionStatus;
import com.example.api.model.enums.VehicleType;
import com.example.api.model.vo.DistributionVo;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * An order moves through review, approval and completion as one row, and races lose cleanly.
 *
 * <p>Against a real MySQL for two reasons a mock cannot cover. The first is the fetch boundary: the
 * controller renders the moved order after the transaction has closed, with open-in-view off, so a
 * missing entity graph only shows up as a LazyInitializationException at runtime. The second is
 * concurrency: every step is a conditional UPDATE whose row count decides the outcome, and whether
 * that holds is a question about two connections, not about which methods were called.
 */
@Testcontainers
@SpringBootTest
class DistributionStatusIT {

    private static final int THREADS = 8;

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
        r.add("spring.datasource.hikari.maximum-pool-size", () -> THREADS + 2);
    }

    @Autowired DistributionService service;
    @Autowired DistributionRepository distributionRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired VehicleRepository vehicleRepository;

    @Test
    @DisplayName("Approve then complete moves the same order and takes then frees its fleet")
    void approveThenComplete_movesOneOrder() {
        Driver driver = driver();
        Vehicle vehicle = vehicle();
        Distribution filed = service.create(order(driver, vehicle));
        long orders = distributionRepository.count();

        // Rendered outside any transaction, exactly as the controller does.
        DistributionVo approved =
                DistributionVo.from(
                        service.changeStatus(filed.getId(), DistributionStatus.REVIEW_SUCCESS));

        assertThat(approved.id()).isEqualTo(filed.getId());
        assertThat(approved.status()).isEqualTo(DistributionStatus.REVIEW_SUCCESS);
        assertThat(approved.driver().name()).isEqualTo(driver.getName());
        assertThat(driving(driver)).isTrue();
        assertThat(driving(vehicle)).isTrue();

        DistributionVo finished =
                DistributionVo.from(service.changeStatus(filed.getId(), DistributionStatus.END));

        assertThat(finished.status()).isEqualTo(DistributionStatus.END);
        assertThat(driving(driver)).isFalse();
        assertThat(driving(vehicle)).isFalse();
        // No copy was filed along the way.
        assertThat(distributionRepository.count()).isEqualTo(orders);
    }

    @Test
    @DisplayName("Approving the same order from many requests at once succeeds exactly once")
    void concurrentApprovalOfOneOrder_succeedsOnce() throws Exception {
        Distribution filed = service.create(order(driver(), vehicle()));

        List<ErrorCode> outcomes =
                race(() -> service.changeStatus(filed.getId(), DistributionStatus.REVIEW_SUCCESS));

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).isNotEmpty();
        assertThat(status(filed)).isEqualTo(DistributionStatus.REVIEW_SUCCESS);
    }

    @Test
    @DisplayName("Two orders sharing a driver cannot both be approved, and the loser takes nothing")
    void concurrentApprovalSharingADriver_admitsOne() throws Exception {
        Driver shared = driver();
        List<Vehicle> vehicles = new ArrayList<>();
        List<Distribution> orders = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Vehicle v = vehicle();
            vehicles.add(v);
            orders.add(service.create(order(shared, v)));
        }

        List<Callable<?>> approvals = new ArrayList<>();
        for (Distribution o : orders) {
            approvals.add(() -> service.changeStatus(o.getId(), DistributionStatus.REVIEW_SUCCESS));
        }
        List<ErrorCode> outcomes = race(approvals);

        assertThat(outcomes).filteredOn(o -> o == null).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o != null).containsOnly(ErrorCode.DRIVER_UNAVAILABLE);
        // Every refused approval rolled back whole: its order is still under review and its
        // vehicle was never taken. Only the winner's truck is out.
        long approved =
                orders.stream().filter(o -> status(o) == DistributionStatus.REVIEW_SUCCESS).count();
        long trucksOut = vehicles.stream().filter(this::driving).count();
        assertThat(approved).isEqualTo(1);
        assertThat(trucksOut).isEqualTo(1);
    }

    // --- fixtures ----------------------------------------------------------------------------

    private Driver driver() {
        Driver d = new Driver();
        d.setName("race-driver-" + UUID.randomUUID().toString().substring(0, 8));
        return driverRepository.saveAndFlush(d);
    }

    private Vehicle vehicle() {
        Vehicle v = new Vehicle();
        v.setNumber("RACE-" + UUID.randomUUID().toString().substring(0, 8));
        v.setType(VehicleType.TRUCK);
        return vehicleRepository.saveAndFlush(v);
    }

    private static Distribution order(Driver driver, Vehicle vehicle) {
        Driver d = new Driver();
        d.setId(driver.getId());
        Vehicle v = new Vehicle();
        v.setId(vehicle.getId());

        Distribution o = new Distribution();
        o.setDriver(d);
        o.setVehicle(v);
        o.setPhone("000-0000-0000");
        o.setAddress("race test");
        o.setTime(LocalDateTime.now());
        o.setStatus(DistributionStatus.REVIEWING);
        return o;
    }

    private boolean driving(Driver driver) {
        return driverRepository.findById(driver.getId()).orElseThrow().isDriving();
    }

    private boolean driving(Vehicle vehicle) {
        return vehicleRepository.findById(vehicle.getId()).orElseThrow().isDriving();
    }

    private DistributionStatus status(Distribution order) {
        return distributionRepository.findById(order.getId()).orElseThrow().getStatus();
    }

    private static List<ErrorCode> race(Callable<?> call) throws Exception {
        List<Callable<?>> calls = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            calls.add(call);
        }
        return race(calls);
    }

    /**
     * Runs the calls on their own threads, released together. Each outcome is null for success or
     * the error code it was refused with; anything that is not a BizException fails the test.
     */
    private static List<ErrorCode> race(List<Callable<?>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch ready = new CountDownLatch(calls.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<ErrorCode>> futures = new ArrayList<>();
            for (Callable<?> call : calls) {
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    go.await();
                                    try {
                                        call.call();
                                        return null;
                                    } catch (BizException e) {
                                        return e.getErrorCode();
                                    }
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<ErrorCode> outcomes = new ArrayList<>();
            for (Future<ErrorCode> f : futures) {
                try {
                    outcomes.add(f.get(60, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    throw new AssertionError(
                            "a racing transition failed unexpectedly", e.getCause());
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
