package com.example.api.repository;

import com.example.api.model.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Inventory findByWarehouseIdAndCommodityId(String warehouseId, String commodityId);

    List<Inventory> findAllByCommodityId(String commodityId);

    List<Inventory> findAllByWarehouseId(String warehouseId);

}
