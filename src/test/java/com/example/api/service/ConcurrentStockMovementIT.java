package com.example.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WarehouseRepository;
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
 * Stock movements stay correct when they race.
 *
 * <p>A movement used to read the stock row, compare in Java and write the result back. Two requests
 * that both read 10 both passed a "10 &gt;= 8" check and both wrote 2: sixteen units shipped out of
 * ten, and the stock screen still said two were left. The transaction did not help - it makes the
 * three writes commit together, it does not stop a second transaction reading the same row before
 * the first has written it, and MySQL's default REPEATABLE READ allows exactly that.
 *
 * <p>This has to be an integration test against a real MySQL. The defect lives in the gap between a
 * SELECT and an UPDATE on two connections, and a mocked repository has neither; the unit tests for
 * this service can only check what is written, never what happens when two writers interleave.
 *
 * <p>Each test releases every request at once from a latch, with more requests than the stock can
 * satisfy, and then checks the three things that must agree: the warehouse row, the commodity
 * total, and the number of movement records.
 */
@Testcontainers
@SpringBootTest
class ConcurrentStockMovementIT {

    private static final int THREADS = 16;

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("loggi");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("jwt.secret", () -> "integration-test-secret-at-least-32-bytes");
        // One connection per racing request, so the race is between transactions rather than
        // between threads queueing for a connection.
        r.add("spring.datasource.hikari.maximum-pool-size", () -> THREADS + 2);
    }

    @Autowired InventoryRecordService service;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired CommodityRepository commodityRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryRecordRepository recordRepository;

    @Test
    @DisplayName("Concurrent outbound movements never ship more than the stock held")
    void concurrentOutNeverOversells() throws Exception {
        Stock stock = stock(10);

        // 16 requests for 1 unit each against 10 units: exactly ten can succeed.
        List<Outcome> outcomes = race(() -> service.out(movement(stock, 1)));

        assertThat(outcomes).filteredOn(o -> o == Outcome.OK).hasSize(10);
        assertThat(outcomes).filteredOn(o -> o == Outcome.INSUFFICIENT_STOCK).hasSize(6);
        assertThat(inventoryCount(stock)).isZero();
        assertThat(commodityCount(stock)).isZero();
        assertThat(recordRepository.findAllByCommodityId(stock.commodityId())).hasSize(10);
    }

    @Test
    @DisplayName("Concurrent large outbound movements cannot both pass the stock check")
    void concurrentLargeOutAdmitsOnlyOne() throws Exception {
        // The shape from the bug report: two requests for 8 against 10. Read-compare-write let
        // every one of them through and left 2 on the books.
        Stock stock = stock(10);

        List<Outcome> outcomes = race(() -> service.out(movement(stock, 8)));

        assertThat(outcomes).filteredOn(o -> o == Outcome.OK).hasSize(1);
        assertThat(inventoryCount(stock)).isEqualTo(2);
        assertThat(commodityCount(stock)).isEqualTo(2);
    }

    @Test
    @DisplayName("Two requests a few milliseconds apart cannot both pass the stock check")
    void staggeredOutNeverOversells() throws Exception {
        // The window the old code actually oversold in. Measured against it before this fix,
        // two requests for 8 against 10 released together deadlocked (the loser got a 500);
        // with the second delayed by 1 to 10 ms, 7 of those 10 rounds let both through -
        // sixteen units out of ten, two movement records, and the books still saying 2. From
        // 11 ms on the first had committed before the second read. So every delay in that
        // range is tried here, rather than trusting one release to land in it.
        for (int delayMs = 0; delayMs <= 15; delayMs++) {
            Stock stock = stock(10);
            int delay = delayMs;

            List<Outcome> outcomes =
                    race(
                            2,
                            index -> {
                                if (index == 1) {
                                    Thread.sleep(delay);
                                }
                                return service.out(movement(stock, 8));
                            });

            assertThat(outcomes).as("delay %d ms", delay).containsOnlyOnce(Outcome.OK);
            assertThat(inventoryCount(stock)).as("delay %d ms", delay).isEqualTo(2);
            assertThat(commodityCount(stock)).as("delay %d ms", delay).isEqualTo(2);
            assertThat(recordRepository.findAllByCommodityId(stock.commodityId()))
                    .as("delay %d ms", delay)
                    .hasSize(1);
        }
    }

    @Test
    @DisplayName("Concurrent inbound movements are all counted, none overwritten")
    void concurrentInLosesNoUpdate() throws Exception {
        // The same race in the other direction does not oversell, it forgets: two +5s that both
        // start from 10 both write 15.
        Stock stock = stock(10);

        List<Outcome> outcomes = race(() -> service.in(movement(stock, 5)));

        assertThat(outcomes).containsOnly(Outcome.OK);
        assertThat(inventoryCount(stock)).isEqualTo(10 + 5 * THREADS);
        assertThat(commodityCount(stock)).isEqualTo(10 + 5 * THREADS);
    }

    // --- fixtures ----------------------------------------------------------------------------

    private record Stock(String warehouseId, String commodityId) {}

    private enum Outcome {
        OK,
        INSUFFICIENT_STOCK
    }

    /** A fresh warehouse holding {@code count} of a fresh commodity, and nothing else. */
    private Stock stock(int count) {
        String tag = UUID.randomUUID().toString().substring(0, 8);

        Warehouse warehouse = new Warehouse();
        warehouse.setName("race-warehouse-" + tag);
        warehouse = warehouseRepository.saveAndFlush(warehouse);

        Commodity commodity = new Commodity();
        commodity.setName("race-commodity-" + tag);
        commodity.setCount(count);
        commodity.setPrice(java.math.BigDecimal.ONE);
        commodity = commodityRepository.saveAndFlush(commodity);

        Inventory inventory = new Inventory();
        inventory.setWarehouse(warehouse);
        inventory.setCommodity(commodity);
        inventory.setName(commodity.getName());
        inventory.setCount(count);
        inventoryRepository.saveAndFlush(inventory);

        return new Stock(warehouse.getId(), commodity.getId());
    }

    /** A movement as a request carries one: references holding nothing but an id. */
    private static InventoryRecord movement(Stock stock, int count) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(stock.warehouseId());
        Commodity commodity = new Commodity();
        commodity.setId(stock.commodityId());

        InventoryRecord record = new InventoryRecord();
        record.setWarehouse(warehouse);
        record.setCommodity(commodity);
        record.setCount(count);
        return record;
    }

    private int inventoryCount(Stock stock) {
        return inventoryRepository
                .findByWarehouseIdAndCommodityId(stock.warehouseId(), stock.commodityId())
                .getCount();
    }

    private int commodityCount(Stock stock) {
        return commodityRepository.findById(stock.commodityId()).orElseThrow().getCount();
    }

    /**
     * Runs the same movement on {@link #THREADS} threads, all released at the same instant.
     *
     * <p>Anything other than success or a refusal for insufficient stock - a deadlock, a lock wait
     * timeout, a constraint violation - is rethrown, so it fails the test rather than being counted
     * as either outcome.
     */
    private static List<Outcome> race(Callable<?> movement) throws Exception {
        return race(THREADS, index -> movement.call());
    }

    /** A movement that knows which of the racing requests it is. */
    @FunctionalInterface
    private interface IndexedMovement {
        Object run(int index) throws Exception;
    }

    private static List<Outcome> race(int threads, IndexedMovement movement) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    go.await();
                                    try {
                                        movement.run(index);
                                        return Outcome.OK;
                                    } catch (BizException e) {
                                        if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                                            return Outcome.INSUFFICIENT_STOCK;
                                        }
                                        throw e;
                                    }
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> f : futures) {
                try {
                    outcomes.add(f.get(60, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    throw new AssertionError("a racing movement failed unexpectedly", e.getCause());
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
