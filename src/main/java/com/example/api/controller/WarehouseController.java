package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.WarehouseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_WAREHOUSE')")
public class WarehouseController {

    @Resource
    private WarehouseService warehouseService;

    @Log(module = "仓库管理",type = BusinessType.INSERT)
    @PostMapping("")
    public Warehouse save(@RequestBody Warehouse warehouse) {
        return warehouseService.save(warehouse);
    }

    @Log(module = "仓库管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<Warehouse> findAll() {
        return warehouseService.findAll();
    }

    @Log(module = "仓库管理",type = BusinessType.DELETE)
    @DeleteMapping("")
    public void delete(String id) {
        warehouseService.delete(id);
    }

}
