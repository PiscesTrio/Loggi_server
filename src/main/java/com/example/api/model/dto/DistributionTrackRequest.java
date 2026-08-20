package com.example.api.model.dto;

import com.example.api.model.enums.DistributionStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A new point on a delivery's trail.
 *
 * <p>The parent is named by id. It had to be sent as an object once the entity became the request
 * type - see DistributionTrackVo for why - and this is the shape that was always meant.
 *
 * <p>No time field: the moment is recorded by the server. A client-supplied timestamp on a tracking
 * record is a client asserting where a vehicle was and when, which is precisely the claim the
 * record exists to make independently.
 */
@Data
public class DistributionTrackRequest {

    @NotBlank(message = "配送单不能为空")
    private String distributionId;

    @DecimalMin(value = "-90.0", message = "纬度超出范围")
    @DecimalMax(value = "90.0", message = "纬度超出范围")
    private double lat;

    @DecimalMin(value = "-180.0", message = "经度超出范围")
    @DecimalMax(value = "180.0", message = "经度超出范围")
    private double lng;

    private String location;

    @NotNull(message = "状态不能为空")
    private DistributionStatus status;
}
