package com.example.api.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

/** What a caller sends to record a sale. */
@Data
public class SaleRequest {

    @NotBlank(message = "company is required")
    private String company;

    private String number;

    @NotBlank(message = "commodityId is required")
    private String commodity;

    @NotNull(message = "count is required")
    @Positive(message = "count must be greater than zero")
    private Integer count;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", message = "price cannot be negative")
    private BigDecimal price;

    private String phone;

    private String description;

    private boolean pay;
}
