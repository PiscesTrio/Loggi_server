package com.example.api.model.vo;

import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.entity.Warehouse;
import com.example.api.model.enums.DistributionStatus;
import java.time.LocalDateTime;

/**
 * A delivery order, as the API describes one.
 *
 * <p>This replaces serialising the entity, and it settles something S09 left awkward. Once the
 * order pointed at real rows, returning the entity meant returning whole Driver, Vehicle and
 * Warehouse objects nested inside every order - each with its own timestamps, its own id-card
 * number, its own everything - because Jackson serialises what the object graph holds, not what a
 * caller needs. The alternative available at the time was to emit a bare id, which took the
 * driver's name away from a screen that displays it.
 *
 * <p>A view type does not have to choose. Each reference becomes a summary carrying the two or
 * three fields a client actually renders, and the fields it does not need never leave the server.
 * The old response had both a {@code did} and a {@code driver} holding a copied name; this has one
 * driver, and the name in it is the driver's current name rather than whatever was copied when the
 * order was filed.
 */
public record DistributionVo(
        String id,
        DriverSummary driver,
        VehicleSummary vehicle,
        WarehouseSummary warehouse,
        String phone,
        String address,
        boolean urgent,
        String care,
        LocalDateTime time,
        DistributionStatus status,
        double fromLat,
        double fromLng,
        double toLat,
        double toLng,
        LocalDateTime createAt) {

    /** Enough to name a driver on a card, and nothing else about them. */
    public record DriverSummary(String id, String name, String phone) {
        static DriverSummary from(Driver driver) {
            return driver == null
                    ? null
                    : new DriverSummary(driver.getId(), driver.getName(), driver.getPhone());
        }
    }

    public record VehicleSummary(String id, String number, String type) {
        static VehicleSummary from(Vehicle vehicle) {
            return vehicle == null
                    ? null
                    : new VehicleSummary(vehicle.getId(), vehicle.getNumber(), vehicle.getType());
        }
    }

    public record WarehouseSummary(String id, String name) {
        static WarehouseSummary from(Warehouse warehouse) {
            return warehouse == null
                    ? null
                    : new WarehouseSummary(warehouse.getId(), warehouse.getName());
        }
    }

    /**
     * Reads every association, so it must be called while they are loaded.
     *
     * <p>{@code DistributionRepository.findAll} declares an entity graph for exactly this reason.
     * Mapping outside a transaction without one throws LazyInitializationException - which is the
     * same failure that used to happen during serialisation, moved somewhere a stack trace points
     * at the cause.
     */
    public static DistributionVo from(Distribution d) {
        return new DistributionVo(
                d.getId(),
                DriverSummary.from(d.getDriver()),
                VehicleSummary.from(d.getVehicle()),
                WarehouseSummary.from(d.getWarehouse()),
                d.getPhone(),
                d.getAddress(),
                d.isUrgent(),
                d.getCare(),
                d.getTime(),
                d.getStatus(),
                d.getFromLat(),
                d.getFromLng(),
                d.getToLat(),
                d.getToLng(),
                d.getCreateAt());
    }
}
