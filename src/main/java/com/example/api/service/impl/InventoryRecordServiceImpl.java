package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.InventoryType;
import com.example.api.model.vo.CommodityChartVo;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WarehouseRepository;
import com.example.api.service.InventoryRecordService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryRecordServiceImpl implements InventoryRecordService {

    @Resource private InventoryRepository inventoryRepository;

    @Resource private CommodityRepository commodityRepository;

    @Resource private InventoryRecordRepository recordRepository;

    @Resource private WarehouseRepository warehouseRepository;

    @Override
    public List<CommodityChartVo> analyzeCommodity(InventoryType type) {
        List<CommodityChartVo> result = new ArrayList<>();
        List<InventoryRecord> all = recordRepository.findAllByType(type);
        Map<String, Integer> map = new HashMap<>();
        for (InventoryRecord r : all) {
            map.merge(r.getName(), r.getCount(), Integer::sum);
        }
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            result.add(new CommodityChartVo(e.getValue(), e.getKey()));
        }
        return result;
    }

    @Override
    public List<InventoryRecord> findAllByWarehouseId(String wid) {
        return recordRepository.findAllByWarehouseId(wid);
    }

    @Override
    public List<InventoryRecord> findAllByCommodityId(String cid) {
        return recordRepository.findAllByCommodityId(cid);
    }

    /**
     * Moves stock out of a warehouse.
     *
     * <p>Three rows change together: the commodity total, the warehouse inventory, and the movement
     * record. Without a transaction each save committed on its own, so a failure between them left
     * the commodity decremented while the warehouse still showed the old count - a discrepancy
     * nothing in this system can detect afterwards, because the movement record that would explain
     * it is exactly the write that did not happen.
     *
     * <p>The transaction is not what stops two requests overselling, though. This used to read the
     * stock row, compare in Java and save the difference, and two concurrent requests for 8 against
     * 10 both read 10, both passed, and both wrote 2. Released at exactly the same moment they
     * deadlocked instead - both took a shared lock on the commodity row through the movement
     * record's foreign key, then both asked to upgrade it - and the loser got a 500. Measured
     * before this change: overlapping requests deadlocked, requests 1 to 10 ms apart oversold in 7
     * of 10 rounds. The check now lives inside the UPDATE ({@code ... where count >= :quantity}),
     * and the number of rows it changed is the answer: the database serialises the two statements
     * on the row lock, so the second sees what the first left. See ConcurrentStockMovementIT, which
     * races sixteen of these against a real MySQL.
     *
     * <p>Both directions lock the inventory row before the commodity row. A fixed order means two
     * movements can wait for each other but never deadlock.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecord out(InventoryRecord record) {
        requirePositiveCount(record);
        String warehouseId = idOf(record.getWarehouse());
        String commodityId = idOf(record.getCommodity());
        int quantity = record.getCount();
        LocalDateTime now = LocalDateTime.now();

        if (inventoryRepository.withdraw(warehouseId, commodityId, quantity, now) == 0) {
            // Zero rows changed means one of two things, and only a second look can say which.
            // Reading after the failed write rather than before keeps the check off the hot
            // path: a successful movement costs one statement, not a read and a write.
            if (inventoryRepository.findByWarehouseIdAndCommodityId(warehouseId, commodityId)
                    == null) {
                throw new BizException(
                        ErrorCode.COMMODITY_NOT_IN_WAREHOUSE,
                        "the warehouse does not hold that commodity");
            }
            // 409, not 400: the request is well formed and would be valid against a
            // different stock level. The caller's next move is to ask for less, not to fix
            // the request.
            throw new BizException(ErrorCode.INSUFFICIENT_STOCK, "not enough stock to send out");
        }

        if (commodityRepository.adjustCount(commodityId, -quantity, now) == 0) {
            // The warehouse had the stock but the commodity total says it does not exist - the
            // two counters disagree. Refusing rolls back the withdrawal above rather than
            // driving the total negative and making the disagreement permanent.
            throw new BizException(ErrorCode.INSUFFICIENT_STOCK, "not enough stock to send out");
        }

        record.setType(InventoryType.OUT);
        record.setName(findCommodity(commodityId).getName());
        return recordRepository.save(record);
    }

    /**
     * Moves stock into a warehouse, creating the inventory row on first arrival.
     *
     * <p>Atomic for the same reason as {@link #out}: a read followed by a save lost one of two
     * concurrent deposits. The one race an UPDATE cannot cover is the very first arrival, where
     * there is no row yet to lock; two of those both insert, and the unique key on (warehouse,
     * commodity) refuses the second with a 409 the caller can retry, rather than leaving two rows
     * that each hold half the stock.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecord in(InventoryRecord record) {
        requirePositiveCount(record);
        String warehouseId = idOf(record.getWarehouse());
        String commodityId = idOf(record.getCommodity());
        int quantity = record.getCount();
        LocalDateTime now = LocalDateTime.now();

        Commodity commodity = findCommodity(commodityId);

        if (inventoryRepository.deposit(warehouseId, commodityId, quantity, now) == 0) {
            Inventory inventory = new Inventory();
            inventory.setCommodity(commodity);
            inventory.setWarehouse(requireWarehouse(warehouseId));
            inventory.setCount(quantity);
            inventory.setName(commodity.getName());
            // Flushed here so a concurrent first arrival fails on the unique key now, before
            // the commodity total below has been moved for a row that will not exist.
            inventoryRepository.saveAndFlush(inventory);
        }

        commodityRepository.adjustCount(commodityId, quantity, now);

        record.setType(InventoryType.IN);
        // The name is copied from the commodity this record already points at, not taken
        // from the caller. It is a denormalised copy that the stock screen and the chart
        // legend render, and asking the client to supply it meant the client could get it
        // wrong or omit it — which it did: a movement submitted without changing the
        // dropdown sent no name at all, and the chart drew a slice with no label.
        record.setName(commodity.getName());
        return recordRepository.save(record);
    }

    /**
     * The commodity, or a 404 naming what was missing.
     *
     * <p>Both methods used to write {@code if (optional == null) throw ...} around a {@link
     * Optional} returned by {@code findById}, which is never null - the branch could not run, and
     * the {@code optional.get()} on the next line threw NoSuchElementException instead. That is not
     * a worse message, it is a different outcome: a guard that reads like validation while the
     * actual failure escapes past it.
     */
    /**
     * The id a request named, out of the reference it arrived as.
     *
     * <p>A movement arrives carrying {@code {"warehouse": {"id": "..."}}} and nothing more, because
     * the client is naming a row rather than sending one. The service looks the real row up before
     * writing anything — which it has to, now that a foreign key would otherwise refuse the write
     * with an error that cannot say which id was wrong.
     */
    private static String idOf(Warehouse warehouse) {
        return warehouse == null ? null : warehouse.getId();
    }

    private static String idOf(Commodity commodity) {
        return commodity == null ? null : commodity.getId();
    }

    private Warehouse requireWarehouse(String wid) {
        return warehouseRepository
                .findById(wid == null ? "" : wid)
                .orElseThrow(
                        () ->
                                new BizException(
                                        ErrorCode.WAREHOUSE_NOT_FOUND,
                                        "no warehouse with id " + wid));
    }

    private Commodity findCommodity(String cid) {
        return commodityRepository
                .findById(cid == null ? "" : cid)
                .orElseThrow(
                        () ->
                                new BizException(
                                        ErrorCode.COMMODITY_NOT_FOUND,
                                        "no commodity with id " + cid));
    }

    /**
     * A movement of zero or fewer units.
     *
     * <p>Unchecked, an inbound record with a negative count silently drains stock through the code
     * path named "in", and the movement record left behind claims the opposite of what happened.
     */
    private void requirePositiveCount(InventoryRecord record) {
        if (record.getCount() == null || record.getCount() <= 0) {
            throw new BizException(
                    ErrorCode.QUANTITY_NOT_POSITIVE, "count must be greater than zero");
        }
    }
}
