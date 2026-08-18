package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.DistributionStatus;
import com.example.api.model.enums.BusincessType;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.service.DistributionService;
import com.example.api.service.DistributionStatusService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    @Resource
    private DistributionService distributionService;

    @Resource
    private DistributionStatusService distributionStatusService;

    @Resource
    private DriverRepository driverRepository;

    @Resource
    private VehicleRepository vehicleRepository;

    @Log(moudle = "配送管理",type = BusincessType.INSERT)
    @PostMapping("")
    public Distribution save(@RequestBody Distribution distribution) {
        return distributionService.save(distribution);
    }

    @Log(moudle = "配送管理",type = BusincessType.QUERY)
    @GetMapping("")
    public List<Distribution> findAll() {
        return distributionService.findAll();
    }

    @GetMapping("can")
    public Map<String, Object> can() {
        Map<String, Object> map = new HashMap<>();
        map.put("drivers", driverRepository.findAllByDriving(false));
        map.put("vehicles", vehicleRepository.findAllByDriving(false));
        return map;
    }

    @GetMapping("status")
    public List<DistributionStatus> getStatus(@RequestParam String dis){
        return distributionStatusService.findByDisId(dis);
    }

    @Log(moudle = "运输状态",type = BusincessType.INSERT)
    @PostMapping("status")
    public DistributionStatus saveStatus(@RequestBody DistributionStatus distributionStatus){
        return distributionStatusService.save(distributionStatus);
    }



}
