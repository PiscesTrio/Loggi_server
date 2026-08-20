package com.example.api.controller;

import com.example.api.model.dto.InventoryMovementRequest;
import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.InventoryRecord;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.InventoryType;
import com.example.api.model.vo.CommodityChartVo;
import com.example.api.model.vo.InventoryRecordVo;
import com.example.api.model.vo.InventoryVo;
import com.example.api.service.InventoryRecordService;
import com.example.api.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Inventory",
        description = "Stock levels per warehouse, and the movements that change them.")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Resource private InventoryService inventoryService;

    @Resource private InventoryRecordService recordService;

    @GetMapping("")
    public List<InventoryVo> findAll() {
        return inventoryService.findAll().stream().map(InventoryVo::from).toList();
    }

    @GetMapping("analyze")
    public List<CommodityChartVo> analyze(InventoryType type) {
        return recordService.analyzeCommodity(type);
    }

    // takes a warehouse id
    // list the stock held in one warehouse
    @GetMapping("/warehouse/{id}")
    public List<InventoryVo> findByWarehouse(@PathVariable String id) {
        return inventoryService.findByWarehouseId(id).stream().map(InventoryVo::from).toList();
    }

    // takes a commodity id
    // list the stock of one commodity across all warehouses
    @GetMapping("/commodity/{id}")
    public List<InventoryVo> findByCommodity(@PathVariable String id) {
        return inventoryService.findByCommodityId(id).stream().map(InventoryVo::from).toList();
    }

    // takes a warehouse id
    // list inbound/outbound records for the goods in one warehouse
    @GetMapping("/record/warehouse/{id}")
    public List<InventoryRecordVo> findRecordByWarehouse(@PathVariable String id) {
        return recordService.findAllByWarehouseId(id).stream()
                .map(InventoryRecordVo::from)
                .toList();
    }

    // takes a commodity id
    // list inbound/outbound records of one commodity across all warehouses
    @GetMapping("/record/commodity/{id}")
    public List<InventoryRecordVo> findRecordByCommodity(@PathVariable String id) {
        return recordService.findAllByCommodityId(id).stream()
                .map(InventoryRecordVo::from)
                .toList();
    }

    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryRecordVo in(@Valid @RequestBody InventoryMovementRequest request) {
        return InventoryRecordVo.from(recordService.in(toEntity(request)));
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryRecordVo out(@Valid @RequestBody InventoryMovementRequest request) {
        return InventoryRecordVo.from(recordService.out(toEntity(request)));
    }

    /**
     * The request, as the entity the service works with.
     *
     * <p>References become entities holding only an id, which is what InventoryRecordServiceImpl
     * resolves before writing - so a warehouse or commodity that does not exist is a 404 naming it,
     * not a foreign-key violation answered with "this already exists".
     */
    private static InventoryRecord toEntity(InventoryMovementRequest request) {
        InventoryRecord record = new InventoryRecord();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(request.getWarehouseId());
        record.setWarehouse(warehouse);
        Commodity commodity = new Commodity();
        commodity.setId(request.getCommodityId());
        record.setCommodity(commodity);
        record.setCount(request.getCount());
        record.setDescription(request.getDescription());
        return record;
    }
}
