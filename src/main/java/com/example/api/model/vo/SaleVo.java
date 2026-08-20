package com.example.api.model.vo;

import com.example.api.model.entity.Sale;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A sale. No client calls this resource - see the API-only note in the README. */
public record SaleVo(
        String id,
        String company,
        String number,
        String commodity,
        Integer count,
        BigDecimal price,
        String phone,
        String description,
        boolean pay,
        LocalDateTime createAt) {

    public static SaleVo from(Sale s) {
        return new SaleVo(
                s.getId(),
                s.getCompany(),
                s.getNumber(),
                s.getCommodity(),
                s.getCount(),
                s.getPrice(),
                s.getPhone(),
                s.getDescription(),
                s.isPay(),
                s.getCreateAt());
    }
}
