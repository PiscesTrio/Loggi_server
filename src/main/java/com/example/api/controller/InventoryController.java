package com.example.api.controller;

import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.vo.CommodityChartVo;
import com.example.api.service.InventoryRecordService;
import com.example.api.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @Resource
    private InventoryRecordService recordService;

    @GetMapping("")
    public List<Inventory> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("analyze")
    public List<CommodityChartVo> analyze(Integer type) {
        return recordService.analyzeCommodity(type);
    }

    //takes a warehouse id
    //list the stock held in one warehouse
    @GetMapping("/warehouse/{id}")
    public List<Inventory> findByWarehouse(@PathVariable String id) {
        return inventoryService.findByWarehouseId(id);
    }

    //takes a commodity id
    //list the stock of one commodity across all warehouses
    @GetMapping("/commodity/{id}")
    public List<Inventory> findByCommodity(@PathVariable String id) {
        return inventoryService.findByCommodityId(id);
    }

    //takes a warehouse id
    //list inbound/outbound records for the goods in one warehouse
    @GetMapping("/record/warehouse/{id}")
    public List<InventoryRecord> findRecordByWarehouse(@PathVariable String id) {
        return recordService.findAllByWarehouseId(id);
    }

    //takes a commodity id
    //list inbound/outbound records of one commodity across all warehouses
    @GetMapping("/record/commodity/{id}")
    public List<InventoryRecord> findRecordByCommodity(@PathVariable String id) {
        return recordService.findAllByCommodityId(id);
    }

    @PostMapping("/in")
    public InventoryRecord in(@RequestBody InventoryRecord record) {
        return recordService.in(record);
    }

    @PostMapping("/out")
    public InventoryRecord out(@RequestBody InventoryRecord record) {
        return recordService.out(record);
    }


}
