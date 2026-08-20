package com.example.api.service.impl;

import com.example.api.model.entity.Inventory;
import com.example.api.repository.InventoryRepository;
import com.example.api.service.InventoryService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Resource private InventoryRepository inventoryRepository;

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public List<Inventory> findByCommodityId(String cid) {
        return inventoryRepository.findAllByCommodityId(cid);
    }

    @Override
    public List<Inventory> findByWarehouseId(String wid) {
        return inventoryRepository.findAllByWarehouseId(wid);
    }
}
