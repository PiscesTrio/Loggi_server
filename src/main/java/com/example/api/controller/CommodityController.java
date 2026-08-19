package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Commodity;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.CommodityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "Commodities", description = "The catalogue of goods this system moves.")
@RestController
@RequestMapping("/api/commodity")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_COMMODITY')")
public class CommodityController {

    @Resource
    private CommodityService commodityService;

    @Log(module = "商品管理",type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Commodity save(@RequestBody Commodity commodity) {
        return commodityService.save(commodity);
    }

    @Log(module = "商品管理",type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        commodityService.delete(id);
    }

    @Log(module = "商品管理",type = BusinessType.UPDATE)
    @PutMapping("")
    public void update(@RequestBody Commodity commodity) {
        commodityService.update(commodity);
    }

    @Log(module = "商品管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<Commodity> findAll() {
        return commodityService.findAll();
    }

    @Log(module = "商品管理",type = BusinessType.QUERY)
    @GetMapping("/search/{name}")
    public List<Commodity> findByLikeName(@PathVariable String name) {
        return commodityService.findAllByLikeName(name);
    }

    //@Log(module = "商品管理",type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public Commodity findById(@PathVariable String id) {
        return commodityService.findById(id);
    }


}
