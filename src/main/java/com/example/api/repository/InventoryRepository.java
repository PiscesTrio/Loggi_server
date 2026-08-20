package com.example.api.repository;

import com.example.api.model.entity.Inventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Inventory findByWarehouseIdAndCommodityId(String warehouseId, String commodityId);

    List<Inventory> findAllByCommodityId(String commodityId);

    List<Inventory> findAllByWarehouseId(String warehouseId);
}
