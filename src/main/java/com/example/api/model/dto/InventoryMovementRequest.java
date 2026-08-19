package com.example.api.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A request to move stock into or out of a warehouse.
 *
 * <p>Which direction it goes is the endpoint, not a field: {@code /in} and {@code /out}
 * decide, and the service sets the type. A request that could name its own direction could
 * disagree with the path it arrived on.
 *
 * <p>{@code count} is required and positive. It was checked in the service - and had to be,
 * because an inbound record with a negative count silently drains stock through the code
 * path named "in" while the movement record left behind claims the opposite of what
 * happened. Declaring it here means the request is refused before any of that runs; the
 * service keeps its own check, because a service is entitled not to trust its caller.
 */
@Data
public class InventoryMovementRequest {

    @NotBlank(message = "仓库不能为空")
    private String warehouseId;

    @NotBlank(message = "商品不能为空")
    private String commodityId;

    /** Denormalised commodity name, rendered by the stock screen and the chart legend. */
    private String name;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer count;

    private String description;
}
