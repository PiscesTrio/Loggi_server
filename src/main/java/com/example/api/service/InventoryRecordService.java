package com.example.api.service;

import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.enums.InventoryType;
import com.example.api.model.vo.CommodityChartVo;
import java.util.List;

public interface InventoryRecordService {

    // inbound/outbound ranking statistics
    List<CommodityChartVo> analyzeCommodity(InventoryType type);

    List<InventoryRecord> findAllByWarehouseId(String wid);

    List<InventoryRecord> findAllByCommodityId(String cid);

    // outbound
    InventoryRecord out(InventoryRecord record);

    // inbound
    InventoryRecord in(InventoryRecord record);
}
