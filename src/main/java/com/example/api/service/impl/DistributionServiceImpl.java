package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
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
import java.time.LocalDateTime;
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
     * Files an order. It always starts as {@link DistributionStatus#REVIEWING}.
     *
     * <p>This method used to be the only write this API had for orders, and it took whatever status
     * the request carried: a new order could be filed as already approved, or as already finished -
     * which released its driver and vehicle even if they were out on someone else's delivery. And
     * because a request carries no id, a client that "approved" an order by posting it again
     * created a second order and left the first one under review forever. Moving an order is now
     * {@link #changeStatus}; filing one is only filing one.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Distribution create(Distribution distribution) {
        if (distribution.getStatus() != DistributionStatus.REVIEWING) {
            throw new BizException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "a new order starts as REVIEWING, not " + distribution.getStatus());
        }
        resolveReferences(distribution);
        return distributionRepository.save(distribution);
    }

    /**
     * Moves an order one step along REVIEWING → REVIEW_SUCCESS → END.
     *
     * <p>Every write here is a conditional UPDATE whose row count is checked, for the same reason
     * stock movements are: the order's status, and the driver's and vehicle's availability, are
     * each read-then-written state that two requests can race on. Approving the same order twice at
     * once, or two orders that share a driver, used to succeed twice; now the second one finds the
     * condition already false, changes nothing, and is refused - and because it throws, the
     * transaction rolls back whatever it had already moved.
     *
     * <p>Locks are taken in a fixed order - order, then driver, then vehicle - so two transitions
     * can wait for each other but not deadlock.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Distribution changeStatus(String id, DistributionStatus next) {
        Distribution order =
                distributionRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.DISTRIBUTION_NOT_FOUND,
                                                "no order with id " + id));
        DistributionStatus current = order.getStatus();
        String driverId = idOf(order.getDriver());
        String vehicleId = idOf(order.getVehicle());

        if (current == null || !current.canMoveTo(next)) {
            throw illegalTransition(current, next);
        }
        if (distributionRepository.moveStatus(id, current, next, LocalDateTime.now()) == 0) {
            // Someone else moved it between the read above and this write.
            throw illegalTransition(current, next);
        }

        if (next == DistributionStatus.REVIEW_SUCCESS) {
            assign(driverId, vehicleId);
        } else if (next == DistributionStatus.END) {
            release(driverId, vehicleId);
        }

        return distributionRepository.findWithAssociationsById(id).orElseThrow();
    }

    private static BizException illegalTransition(
            DistributionStatus current, DistributionStatus next) {
        return new BizException(
                ErrorCode.INVALID_STATUS_TRANSITION,
                "an order in " + current + " cannot move to " + next);
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
                            .orElseThrow(
                                    () ->
                                            new BizException(
                                                    ErrorCode.WAREHOUSE_NOT_FOUND,
                                                    "no warehouse with id " + warehouseId)));
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
     * <p>The availability check and the write are one statement ({@code ... where driving =
     * false}). Reading {@code isDriving()} first and writing afterwards was a check two concurrent
     * approvals could both pass.
     *
     * <p>There used to be four commented-out lines here reading "if the driver is driving, refuse"
     * - but they sat in the <em>completion</em> branch, where a driver is driving by definition, so
     * restoring them there would have rejected every completion the system can produce. Dead code
     * is not a spare part; it was commented out because it did not work.
     */
    private void assign(String driverId, String vehicleId) {
        if (driverRepository.switchDriving(driverId, false, true) == 0) {
            throw new BizException(ErrorCode.DRIVER_UNAVAILABLE, "that driver is already assigned");
        }
        if (vehicleRepository.switchDriving(vehicleId, false, true) == 0) {
            // Throwing rolls back the driver taken above, so a refused approval leaves the
            // driver free rather than marked busy for a trip that never happened.
            throw new BizException(
                    ErrorCode.VEHICLE_UNAVAILABLE, "that vehicle is already assigned");
        }
    }

    /**
     * Completion: the driver and vehicle go back into the pool.
     *
     * <p>Only reachable from REVIEW_SUCCESS, so the driver and vehicle being released are the ones
     * this order took on approval. Releasing used to be reachable by filing a new order as END,
     * which freed whoever it named - including a driver out on a different delivery.
     */
    private void release(String driverId, String vehicleId) {
        driverRepository.switchDriving(driverId, true, false);
        vehicleRepository.switchDriving(vehicleId, true, false);
    }

    private Driver requireDriver(String did) {
        return driverRepository
                .findById(did == null ? "" : did)
                .orElseThrow(
                        () ->
                                new BizException(
                                        ErrorCode.DRIVER_NOT_FOUND, "no driver with id " + did));
    }

    private Vehicle requireVehicle(String vid) {
        return vehicleRepository
                .findById(vid == null ? "" : vid)
                .orElseThrow(
                        () ->
                                new BizException(
                                        ErrorCode.VEHICLE_NOT_FOUND, "no vehicle with id " + vid));
    }

    @Override
    public List<Distribution> findAll() {
        return distributionRepository.findAll();
    }
}
