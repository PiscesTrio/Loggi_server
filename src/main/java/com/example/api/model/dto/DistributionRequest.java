package com.example.api.model.dto;

import com.example.api.model.enums.CareTag;
import com.example.api.model.enums.DistributionStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;

/**
 * What a caller sends to file or update a delivery order.
 *
 * <p>References arrive as plain ids. The entity was accepted directly before, which meant a request
 * had to name a driver as {@code {"driver": {"id": "..."}}} - an object standing in for an id,
 * because the field's type was an entity. Naming a row by its id is what a request does; the
 * service resolves it.
 *
 * <p>Note what is absent: no {@code id}. Creating a row was a POST carrying an id the client
 * invented, which Hibernate read as "an existing row to update" and refused. The id is the
 * database's to assign.
 */
@Data
public class DistributionRequest {

    @NotBlank(message = "driverId is required")
    private String driverId;

    @NotBlank(message = "vehicleId is required")
    private String vehicleId;

    /** Optional: an order can be filed before its origin is decided. */
    private String warehouseId;

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "address is required")
    private String address;

    private boolean urgent;

    /**
     * Handling instructions. A list, not a comma-joined string.
     *
     * <p>Unknown tags are rejected by deserialisation rather than stored: the field's type is the
     * closed set, so a client cannot invent a ninth.
     */
    private Set<CareTag> care = new LinkedHashSet<>();

    @NotNull(message = "time is required")
    private LocalDateTime time;

    @NotNull(message = "status is required")
    private DistributionStatus status;

    // Bounded because a coordinate outside these is not a coordinate. The origin used to
    // arrive as 0,0 whenever the client forgot to copy it from the selected warehouse -
    // valid as a number, in the Gulf of Guinea as a place.
    @DecimalMin(value = "-90.0", message = "latitude is out of range")
    @DecimalMax(value = "90.0", message = "latitude is out of range")
    private double fromLat;

    @DecimalMin(value = "-180.0", message = "longitude is out of range")
    @DecimalMax(value = "180.0", message = "longitude is out of range")
    private double fromLng;

    @DecimalMin(value = "-90.0", message = "latitude is out of range")
    @DecimalMax(value = "90.0", message = "latitude is out of range")
    private double toLat;

    @DecimalMin(value = "-180.0", message = "longitude is out of range")
    @DecimalMax(value = "180.0", message = "longitude is out of range")
    private double toLng;
}
