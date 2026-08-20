package com.example.api.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

/**
 * What a caller sends to create or update a commodity.
 *
 * <p>No id, no timestamps. Accepting the entity meant a caller could set any field on it, including
 * the ones the server owns: an id on a create is read by Hibernate as "an existing row to update"
 * and refused, which is exactly how creating a delivery order failed silently for the whole life of
 * this project. The timestamps are written by auditing and a value supplied here would be
 * overwritten anyway - accepting it only invites the belief that it meant something.
 */
@Data
public class CommodityRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0", message = "单价不能为负")
    private BigDecimal price;

    private String description;

    // Zero is allowed - a commodity can exist with nothing in stock. Negative is not: there
    // is no such thing as minus three of something on a shelf.
    @PositiveOrZero(message = "库存数量不能为负")
    private int count;
}
