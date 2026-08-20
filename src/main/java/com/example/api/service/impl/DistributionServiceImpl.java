package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.DistributionStatus;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.repository.WarehouseRepository;
import com.example.api.service.DistributionService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributionServiceImpl implements DistributionService {

    @Resource private DistributionRepository distributionRepository;

    @Resource private DriverRepository driverRepository;

    @Resource private VehicleRepository vehicleRepository;

    @Resource private WarehouseRepository warehouseRepository;

    /**
     * Saves an order and moves the driver and vehicle with it.
     *
     * <p>Approving an order marks both as driving; completing it releases them. Those are two
     * writes plus the order itself, and without a transaction they committed separately: a failure
     * after the driver update left a driver marked busy for a trip that was never recorded, and
     * {@code /api/distribution/can} - which lists whoever is not driving - hid them from every
     * later dispatch with nothing to explain why.
     *
     * <p>The status codes were bare 1 and 2. {@link DistributionStatus} had spelled them
     * REVIEW_SUCCESS and END since the first version of this project and was referenced from
     * nowhere.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Distribution save(Distribution distribution) {
        resolveReferences(distribution);

        DistributionStatus status = distribution.getStatus();
        if (status == DistributionStatus.REVIEW_SUCCESS) {
            assign(distribution);
        } else if (status == DistributionStatus.END) {
            release(distribution);
        }
        return distributionRepository.save(distribution);
    }

    /**
     * Replaces the references the request named with the rows they name.
     *
     * <p>Necessary now that these are real foreign keys. A request arrives carrying an id and
     * nothing else; if it is written straight through, the database is the thing that discovers the
     * id does not exist, and a constraint violation cannot say which of the three was wrong.
     * Resolving here turns that into a 404 naming the id.
     *
     * <p>It also closes a gap that predates the associations: only approval and completion checked
     * the driver and vehicle existed. Creating an order - the common case - checked nothing, so an
     * order could be filed against a driver who was never hired.
     */
    private void resolveReferences(Distribution distribution) {
        distribution.setDriver(requireDriver(idOf(distribution.getDriver())));
        distribution.setVehicle(requireVehicle(idOf(distribution.getVehicle())));

        // The origin warehouse is optional on an order, so absence is allowed; a named one
        // that does not exist is not.
        String warehouseId = idOf(distribution.getWarehouse());
        if (warehouseId != null) {
            distribution.setWarehouse(
                    warehouseRepository
                            .findById(warehouseId)
                            .orElseThrow(() -> new BizException(404, "不存在的仓库id: " + warehouseId)));
        }
    }

    private static String idOf(Object entity) {
        if (entity instanceof Driver driver) return driver.getId();
        if (entity instanceof Vehicle vehicle) return vehicle.getId();
        if (entity instanceof Warehouse warehouse) return warehouse.getId();
        return null;
    }

    /**
     * Approval: the driver and vehicle are taken.
     *
     * <p>This is where the availability check belongs, and it is worth saying why it was not simply
     * uncommented where it was found. The commented-out lines sat in the <em>completion</em> branch
     * and read "if the driver is driving, refuse" - but a driver completing a delivery is by
     * definition driving, having been marked so on approval, so restoring them there would have
     * rejected every completion the system can produce. Dead code is not a spare part; it was
     * commented out because it did not work, and the question is what it was reaching for rather
     * than where it happened to sit.
     *
     * <p>Existence is checked in both directions. A missing id used to reach {@code updateDriving},
     * whose UPDATE matched no rows and reported success.
     */
    private void assign(Distribution distribution) {
        Driver driver = distribution.getDriver();
        Vehicle vehicle = distribution.getVehicle();

        // Re-approving an order already approved will be refused here. That is the correct
        // answer for a second driver being handed a busy truck; a retry of the same
        // approval is the case it also catches, and telling the caller the assignment did
        // not go through is safer than silently reassigning.
        if (driver.isDriving()) {
            throw new BizException(409, "司机当前不可用");
        }
        if (vehicle.isDriving()) {
            throw new BizException(409, "货车当前不可用");
        }

        driverRepository.updateDriving(true, driver.getId());
        vehicleRepository.updateDriving(true, vehicle.getId());
    }

    /** Completion: the driver and vehicle go back into the pool. */
    private void release(Distribution distribution) {
        driverRepository.updateDriving(false, distribution.getDriver().getId());
        vehicleRepository.updateDriving(false, distribution.getVehicle().getId());
    }

    private Driver requireDriver(String did) {
        return driverRepository
                .findById(did == null ? "" : did)
                .orElseThrow(() -> new BizException(404, "不存在的司机id: " + did));
    }

    private Vehicle requireVehicle(String vid) {
        return vehicleRepository
                .findById(vid == null ? "" : vid)
                .orElseThrow(() -> new BizException(404, "不存在的货车id: " + vid));
    }

    @Override
    public List<Distribution> findAll() {
        return distributionRepository.findAll();
    }
}
