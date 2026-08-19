package com.example.api.model.vo;

import com.example.api.model.entity.Commodity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A commodity in the catalogue. */
public record CommodityVo(
        String id,
        String name,
        BigDecimal price,
        String description,
        int count,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updateAt) {

    public static CommodityVo from(Commodity c) {
        return new CommodityVo(c.getId(), c.getName(), c.getPrice(), c.getDescription(),
                c.getCount(), c.getCreateAt(), c.getUpdateAt());
    }
}
