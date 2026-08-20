package com.example.api.model.dto;

import com.example.api.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * What a caller sends to create a driver.
 *
 * <p>{@code driving} is absent deliberately. Whether a driver is out on a delivery is decided by
 * approving and completing orders, not by whoever is editing the driver record - accepting it here
 * would let a caller mark a busy driver free and dispatch them twice.
 */
@Data
public class DriverRequest {

    @NotBlank(message = "name is required")
    private String name;

    private Gender gender;

    @NotBlank(message = "phone is required")
    private String phone;

    private String address;

    private String idCard;

    private String license;

    private String score;
}
