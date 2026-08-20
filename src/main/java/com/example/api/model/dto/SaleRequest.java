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

    @NotBlank(message = "客户公司不能为空")
    private String company;

    private String number;

    @NotBlank(message = "商品不能为空")
    private String commodity;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于 0")
    private Integer count;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.0", message = "金额不能为负")
    private BigDecimal price;

    private String phone;

    private String description;

    private boolean pay;
}
