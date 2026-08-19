package com.example.api.model.vo;

import com.example.api.model.entity.Vehicle;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** A vehicle in the fleet. */
public record VehicleVo(
        String id,
        String number,
        String type,
        boolean driving,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createAt) {

    public static VehicleVo from(Vehicle v) {
        return new VehicleVo(v.getId(), v.getNumber(), v.getType(), v.isDriving(), v.getCreateAt());
    }
}
