package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.enums.InventoryType;
import com.example.api.model.vo.CommodityChartVo;
import com.example.api.repository.CommodityRepository;
import com.example.api.repository.InventoryRecordRepository;
import com.example.api.repository.InventoryRepository;
import com.example.api.repository.WarehouseRepository;
import com.example.api.service.InventoryRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;

@Service
public class InventoryRecordServiceImpl implements InventoryRecordService {

    /** Stock leaving a warehouse. Persisted on InventoryRecord.type. */
    // TYPE_OUT / TYPE_IN are gone: the meaning of the column is now InventoryType, declared
    // where every reader can see it rather than privately here.

    /** Stock arriving at a warehouse. */


    @Resource
    private InventoryRepository inventoryRepository;

    @Resource
    private CommodityRepository commodityRepository;

    @Resource
    private InventoryRecordRepository recordRepository;

    @Resource
    private WarehouseRepository warehouseRepository;

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
     * <p>Three rows change together: the commodity total, the warehouse inventory, and the
     * movement record. Without a transaction each save committed on its own, so a failure
     * between them left the commodity decremented while the warehouse still showed the old
     * count - a discrepancy nothing in this system can detect afterwards, because the
     * movement record that would explain it is exactly the write that did not happen.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecord out(InventoryRecord record) {
        requirePositiveCount(record);

        Inventory inventory = inventoryRepository.findByWarehouseIdAndCommodityId(idOf(record.getWarehouse()), idOf(record.getCommodity()));
        if (inventory == null) {
            throw new BizException(404, "仓库内不存在该商品");
        }
        if (inventory.getCount() < record.getCount()) {
            // 409, not 400: the request is well formed and would be valid against a
            // different stock level. The caller's next move is to ask for less, not to fix
            // the request.
            throw new BizException(409, "出库失败，库存数量不足");
        }

        Commodity commodity = findCommodity(idOf(record.getCommodity()));
        commodity.setCount(commodity.getCount() - record.getCount());
        commodityRepository.save(commodity);

        inventory.setCount(inventory.getCount() - record.getCount());
        inventoryRepository.save(inventory);

        record.setType(InventoryType.OUT);
        return recordRepository.save(record);
    }

    /** Moves stock into a warehouse, creating the inventory row on first arrival. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryRecord in(InventoryRecord record) {
        requirePositiveCount(record);

        Commodity commodity = findCommodity(idOf(record.getCommodity()));
        commodity.setCount(commodity.getCount() + record.getCount());
        commodityRepository.save(commodity);

        Inventory inventory = inventoryRepository.findByWarehouseIdAndCommodityId(idOf(record.getWarehouse()), idOf(record.getCommodity()));
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setCommodity(commodity);
            inventory.setWarehouse(requireWarehouse(idOf(record.getWarehouse())));
            inventory.setCount(0);
            inventory.setName(record.getName());
        }
        inventory.setCount(inventory.getCount() + record.getCount());
        inventoryRepository.save(inventory);

        record.setType(InventoryType.IN);
        return recordRepository.save(record);
    }

    /**
     * The commodity, or a 404 naming what was missing.
     *
     * <p>Both methods used to write {@code if (optional == null) throw ...} around a
     * {@link Optional} returned by {@code findById}, which is never null - the branch could
     * not run, and the {@code optional.get()} on the next line threw
     * NoSuchElementException instead. That is not a worse message, it is a different
     * outcome: a guard that reads like validation while the actual failure escapes past it.
     */
    /**
     * The id a request named, out of the reference it arrived as.
     *
     * <p>A movement arrives carrying {@code {"warehouse": {"id": "..."}}} and nothing more,
     * because the client is naming a row rather than sending one. The service looks the real
     * row up before writing anything — which it has to, now that a foreign key would
     * otherwise refuse the write with an error that cannot say which id was wrong.
     */
    private static String idOf(Warehouse warehouse) {
        return warehouse == null ? null : warehouse.getId();
    }

    private static String idOf(Commodity commodity) {
        return commodity == null ? null : commodity.getId();
    }

    private Warehouse requireWarehouse(String wid) {
        return warehouseRepository.findById(wid == null ? "" : wid)
                .orElseThrow(() -> new BizException(404, "不存在的仓库id: " + wid));
    }

    private Commodity findCommodity(String cid) {
        return commodityRepository.findById(cid == null ? "" : cid)
                .orElseThrow(() -> new BizException(404, "不存在的商品id: " + cid));
    }

    /**
     * A movement of zero or fewer units.
     *
     * <p>Unchecked, an inbound record with a negative count silently drains stock through
     * the code path named "in", and the movement record left behind claims the opposite of
     * what happened.
     */
    private void requirePositiveCount(InventoryRecord record) {
        if (record.getCount() == null || record.getCount() <= 0) {
            throw new BizException(400, "数量必须大于 0");
        }
    }
}
