package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.DriverRequest;
import com.example.api.model.entity.Driver;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.enums.LogModule;
import com.example.api.model.vo.DriverVo;
import com.example.api.service.DriverService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Drivers", description = "The people who drive.")
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Resource private DriverService driverService;

    @Log(module = LogModule.DRIVER, type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public DriverVo save(@Valid @RequestBody DriverRequest request) {
        return DriverVo.from(driverService.save(toEntity(request)));
    }

    @Log(module = LogModule.DRIVER, type = BusinessType.QUERY)
    @GetMapping("")
    public List<DriverVo> findAll() {
        return driverService.findAll().stream().map(DriverVo::from).toList();
    }

    @Log(module = LogModule.DRIVER, type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public DriverVo findById(@PathVariable String id) {
        return DriverVo.from(driverService.findById(id));
    }

    @Log(module = LogModule.DRIVER, type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        driverService.delete(id);
    }

    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Driver toEntity(DriverRequest request) {
        Driver e = new Driver();
        e.setName(request.getName());
        e.setGender(request.getGender());
        e.setPhone(request.getPhone());
        e.setAddress(request.getAddress());
        e.setIdCard(request.getIdCard());
        e.setLicense(request.getLicense());
        e.setScore(request.getScore());
        return e;
    }
}
