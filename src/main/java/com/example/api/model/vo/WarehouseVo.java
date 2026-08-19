package com.example.api.model.vo;

import com.example.api.model.entity.Warehouse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** A warehouse. Coordinates are WGS-84, which is what the map layer expects. */
public record WarehouseVo(
        String id,
        String name,
        String principle,
        String location,
        double lat,
        double lng,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createAt) {

    public static WarehouseVo from(Warehouse w) {
        return new WarehouseVo(w.getId(), w.getName(), w.getPrinciple(), w.getLocation(),
                w.getLat(), w.getLng(), w.getCreateAt());
    }
}
