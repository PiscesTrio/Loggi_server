package com.example.api.repository;

import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.enums.InventoryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRecordRepository extends JpaRepository<InventoryRecord, String> {

    List<InventoryRecord> findAllByWarehouseId(String warehouseId);

    List<InventoryRecord> findAllByType(InventoryType type);

    List<InventoryRecord> findAllByCommodityId(String commodityId);
}
