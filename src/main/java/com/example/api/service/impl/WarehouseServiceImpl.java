package com.example.api.service.impl;

import com.example.api.model.entity.Warehouse;
import com.example.api.repository.WarehouseRepository;
import com.example.api.service.WarehouseService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Resource
    private WarehouseRepository warehouseRepository;

    @Override
    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Override
    public List<Warehouse> findAll() {
        return warehouseRepository.findAll();
    }

    @Override
    public void delete(String id) {
        warehouseRepository.deleteById(id);
    }

}
