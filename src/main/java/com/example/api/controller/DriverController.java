package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Driver;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.DriverService;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "Drivers", description = "The people who drive.")
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Resource
    private DriverService driverService;

    @Log(module = "驾驶员管理",type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Driver save(@RequestBody Driver driver) {
        return driverService.save(driver);
    }

    @Log(module = "驾驶员管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<Driver> findAll() {
        return driverService.findAll();
    }

    @Log(module = "驾驶员管理",type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public Driver findById(@PathVariable String id) {
        return driverService.findById(id);
    }

    @Log(module = "驾驶员管理",type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        driverService.delete(id);
    }

}
