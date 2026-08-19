package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.VehicleService;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "Vehicles", description = "The fleet.")
@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Resource
    private VehicleService vehicleService;

    @Log(module = "车辆管理",type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
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
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        vehicleService.delete(id);
    }

}
