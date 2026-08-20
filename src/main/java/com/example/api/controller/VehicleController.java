package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.VehicleRequest;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.vo.VehicleVo;
import com.example.api.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vehicles", description = "The fleet.")
@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Resource private VehicleService vehicleService;

    @Log(module = "车辆管理", type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleVo save(@Valid @RequestBody VehicleRequest request) {
        return VehicleVo.from(vehicleService.save(toEntity(request)));
    }

    @Log(module = "车辆管理", type = BusinessType.QUERY)
    @GetMapping("")
    public List<VehicleVo> findAll() {
        return vehicleService.findAll().stream().map(VehicleVo::from).toList();
    }

    @Log(module = "车辆管理", type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public VehicleVo findById(@PathVariable String id) {
        return VehicleVo.from(vehicleService.findById(id));
    }

    @Log(module = "车辆管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        vehicleService.delete(id);
    }

    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Vehicle toEntity(VehicleRequest request) {
        Vehicle e = new Vehicle();
        e.setNumber(request.getNumber());
        e.setType(request.getType());
        return e;
    }
}
