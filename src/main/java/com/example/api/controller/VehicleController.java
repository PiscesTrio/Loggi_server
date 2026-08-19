package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.VehicleService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Resource
    private VehicleService vehicleService;

    @Log(module = "车辆管理",type = BusinessType.INSERT)
    @PostMapping("")
    public Vehicle save(@RequestBody Vehicle vehicle) {
        return vehicleService.save(vehicle);
    }

    @Log(module = "车辆管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<Vehicle> findAll() {
        return vehicleService.findAll();
    }

    @Log(module = "车辆管理",type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public Vehicle findById(@PathVariable String id) {
        return vehicleService.findById(id);
    }

    @Log(module = "车辆管理",type = BusinessType.DELETE)
    @DeleteMapping("")
    public void delete(String id) {
        vehicleService.delete(id);
    }

}
