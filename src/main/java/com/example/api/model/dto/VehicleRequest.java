package com.example.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * What a caller sends to add a vehicle.
 *
 * <p>{@code driving} is absent for the same reason as on a driver: it follows from orders.
 */
@Data
public class VehicleRequest {

    @NotBlank(message = "车牌号不能为空")
    private String number;

    @NotBlank(message = "车辆类型不能为空")
    private String type;
}
