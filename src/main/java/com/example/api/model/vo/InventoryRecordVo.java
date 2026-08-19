package com.example.api.model.vo;

import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.InventoryType;

import java.time.LocalDateTime;

/**
 * One stock movement, in or out.
 */
public record InventoryRecordVo(
        String id,
        String warehouseId,
        String commodityId,
        String name,
        Integer count,
        InventoryType type,
        String description,
        LocalDateTime createAt) {

    public static InventoryRecordVo from(InventoryRecord record) {
        Warehouse warehouse = record.getWarehouse();
        Commodity commodity = record.getCommodity();
        return new InventoryRecordVo(
                record.getId(),
                warehouse == null ? null : warehouse.getId(),
                commodity == null ? null : commodity.getId(),
                record.getName(),
                record.getCount(),
                record.getType(),
                record.getDescription(),
                record.getCreateAt());
    }
}
