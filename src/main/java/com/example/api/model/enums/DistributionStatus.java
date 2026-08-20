package com.example.api.model.enums;

import lombok.Getter;
import lombok.Setter;

/** Distribution order status */
public enum DistributionStatus {

    // Under review
    REVIEWING(0),

    // Review passed
    REVIEW_SUCCESS(1),

    // Order completed
    END(2);

    @Getter @Setter private Integer code;

    DistributionStatus(Integer code) {
        this.code = code;
    }
}
