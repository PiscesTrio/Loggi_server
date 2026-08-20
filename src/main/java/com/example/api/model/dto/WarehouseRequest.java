package com.example.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** What a caller sends to create a warehouse. */
@Data
public class WarehouseRequest {

    @NotBlank(message = "warehouse name is required")
    private String name;

    private String principle;

    @NotBlank(message = "warehouse address is required")
    private String location;

    // Bounded, because the map draws whatever arrives. An unchecked coordinate puts a
    // warehouse in the ocean and the screen simply shows it there.
    @DecimalMin(value = "-90.0", message = "latitude is out of range")
    @DecimalMax(value = "90.0", message = "latitude is out of range")
    private double lat;

    @DecimalMin(value = "-180.0", message = "longitude is out of range")
    @DecimalMax(value = "180.0", message = "longitude is out of range")
    private double lng;
}
