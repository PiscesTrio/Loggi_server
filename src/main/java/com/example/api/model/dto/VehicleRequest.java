package com.example.api.model.dto;

import com.example.api.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * What a caller sends to add a vehicle.
 *
 * <p>{@code driving} is absent for the same reason as on a driver: it follows from orders.
 */
@Data
public class VehicleRequest {

    @NotBlank(message = "number is required")
    private String number;

    // @NotNull, not @NotBlank: blankness is a property of a CharSequence, and Hibernate
    // Validator does not silently ignore a constraint it cannot apply — it throws
    // UnexpectedTypeException when the request is validated, turning every create into a 500.
    // The annotation survived the field's change of type because nothing compiled against it.
    @NotNull(message = "type is required")
    private VehicleType type;
}
