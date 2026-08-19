package com.example.api.model.vo;

import com.example.api.model.entity.Vehicle;

import java.time.LocalDateTime;

/** A vehicle in the fleet. */
public record VehicleVo(
        String id,
        String number,
        String type,
        boolean driving,
        LocalDateTime createAt) {

    public static VehicleVo from(Vehicle v) {
        return new VehicleVo(v.getId(), v.getNumber(), v.getType(), v.isDriving(), v.getCreateAt());
    }
}
