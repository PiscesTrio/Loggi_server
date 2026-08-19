package com.example.api.model.vo;

import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;

import java.util.List;

/**
 * Who and what is free to dispatch.
 *
 * <p>Was a {@code HashMap} with "drivers" and "vehicles" put into it. The client's model for
 * this endpoint declares both lists nullable and then force-unwraps them, which is what a
 * map invites: nothing says the keys are always present, so the caller has to assume, and
 * assuming is how {@code available.drivers!.isEmpty} came to be written.
 */
public record AvailableFleetVo(List<DistributionVo.DriverSummary> drivers,
                               List<DistributionVo.VehicleSummary> vehicles) {

    public static AvailableFleetVo of(List<Driver> drivers, List<Vehicle> vehicles) {
        return new AvailableFleetVo(
                drivers.stream().map(DistributionVo.DriverSummary::from).toList(),
                vehicles.stream().map(DistributionVo.VehicleSummary::from).toList());
    }
}
