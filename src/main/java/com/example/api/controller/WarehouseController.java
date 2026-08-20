package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.WarehouseRequest;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.vo.WarehouseVo;
import com.example.api.service.WarehouseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Warehouses", description = "Sites that hold stock.")
@RestController
@RequestMapping("/api/warehouse")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_WAREHOUSE')")
public class WarehouseController {

    @Resource private WarehouseService warehouseService;

    @Log(module = "仓库管理", type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseVo save(@Valid @RequestBody WarehouseRequest request) {
        return WarehouseVo.from(warehouseService.save(toEntity(request)));
    }

    @Log(module = "仓库管理", type = BusinessType.QUERY)
    @GetMapping("")
    public List<WarehouseVo> findAll() {
        return warehouseService.findAll().stream().map(WarehouseVo::from).toList();
    }

    @Log(module = "仓库管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        warehouseService.delete(id);
    }

    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Warehouse toEntity(WarehouseRequest request) {
        Warehouse e = new Warehouse();
        e.setName(request.getName());
        e.setPrinciple(request.getPrinciple());
        e.setLocation(request.getLocation());
        e.setLat(request.getLat());
        e.setLng(request.getLng());
        return e;
    }
}
