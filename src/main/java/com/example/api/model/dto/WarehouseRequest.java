package com.example.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** What a caller sends to create a warehouse. */
@Data
public class WarehouseRequest {

    @NotBlank(message = "仓库名称不能为空")
    private String name;

    private String principle;

    @NotBlank(message = "仓库地址不能为空")
    private String location;

    // Bounded, because the map draws whatever arrives. An unchecked coordinate puts a
    // warehouse in the ocean and the screen simply shows it there.
    @DecimalMin(value = "-90.0", message = "纬度超出范围")
    @DecimalMax(value = "90.0", message = "纬度超出范围")
    private double lat;

    @DecimalMin(value = "-180.0", message = "经度超出范围")
    @DecimalMax(value = "180.0", message = "经度超出范围")
    private double lng;
}
