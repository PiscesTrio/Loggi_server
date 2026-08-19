package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.DistributionRequest;
import com.example.api.model.dto.DistributionTrackRequest;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.vo.AvailableFleetVo;
import com.example.api.model.vo.DistributionTrackVo;
import com.example.api.model.vo.DistributionVo;
import com.example.api.model.entity.DistributionTrack;
import com.example.api.model.enums.BusinessType;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.service.DistributionService;
import com.example.api.service.DistributionTrackService;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.function.BiConsumer;
import java.util.List;

@Tag(name = "Delivery orders", description = "Orders, the fleet available to carry them, and their tracking trail.")
@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    @Resource
    private DistributionService distributionService;

    @Resource
    private DistributionTrackService distributionTrackService;

    @Resource
    private DriverRepository driverRepository;

    @Resource
    private VehicleRepository vehicleRepository;

    @Log(module = "配送管理",type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public DistributionVo save(@Valid @RequestBody DistributionRequest request) {
        return DistributionVo.from(distributionService.save(toEntity(request)));
    }

    /**
     * The request, as the entity the service works with.
     *
     * <p>References become entities holding nothing but an id, which is exactly what
     * {@code DistributionServiceImpl.save} expects: it resolves each one against the
     * database before writing, so a bad id is a 404 naming it rather than a foreign-key
     * violation that cannot say which of the three was wrong.
     */
    private static Distribution toEntity(DistributionRequest request) {
        Distribution d = new Distribution();
        d.setDriver(reference(new Driver(), request.getDriverId(), Driver::setId));
        d.setVehicle(reference(new Vehicle(), request.getVehicleId(), Vehicle::setId));
        if (request.getWarehouseId() != null) {
            d.setWarehouse(reference(new Warehouse(), request.getWarehouseId(), Warehouse::setId));
        }
        d.setPhone(request.getPhone());
        d.setAddress(request.getAddress());
        d.setUrgent(request.isUrgent());
        d.setCare(request.getCare());
        d.setTime(request.getTime());
        d.setStatus(request.getStatus());
        d.setFromLat(request.getFromLat());
        d.setFromLng(request.getFromLng());
        d.setToLat(request.getToLat());
        d.setToLng(request.getToLng());
        return d;
    }

    private static <T> T reference(T entity, String id, BiConsumer<T, String> setId) {
        setId.accept(entity, id);
        return entity;
    }

    @Log(module = "配送管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<DistributionVo> findAll() {
        return distributionService.findAll().stream().map(DistributionVo::from).toList();
    }

    @GetMapping("can")
    public AvailableFleetVo can() {
        return AvailableFleetVo.of(driverRepository.findAllByDriving(false),
                vehicleRepository.findAllByDriving(false));
    }

    @GetMapping("status")
    public List<DistributionTrackVo> getStatus(@RequestParam String dis){
        return distributionTrackService.findByDisId(dis).stream()
                .map(DistributionTrackVo::from).toList();
    }

    @Log(module = "运输状态",type = BusinessType.INSERT)
    @PostMapping("status")
    public DistributionTrackVo saveStatus(@Valid @RequestBody DistributionTrackRequest request){
        DistributionTrack track = new DistributionTrack();
        Distribution parent = new Distribution();
        parent.setId(request.getDistributionId());
        track.setDistribution(parent);
        track.setLat(request.getLat());
        track.setLng(request.getLng());
        track.setLocation(request.getLocation());
        track.setStatus(request.getStatus());
        return DistributionTrackVo.from(distributionTrackService.save(track));
    }



}
