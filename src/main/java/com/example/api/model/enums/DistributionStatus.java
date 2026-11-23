package com.example.api.model.enums;

/**
 * Where a delivery order is in its life, and where it may go next.
 *
 * <p>The transitions are declared here, on the type, rather than as branches in a service, because
 * they are a property of the status and not of any one caller: an order under review can be
 * approved, an approved order can be completed, and nothing else. A completed order is final.
 */
public enum DistributionStatus {

    /** Filed, waiting for someone to approve it. Every order starts here. */
    REVIEWING,

    /** Approved: its driver and vehicle are out on it. */
    REVIEW_SUCCESS,

    /** Delivered: its driver and vehicle are free again. */
    END;

    /** Whether an order in this status may move to {@code next}. */
    public boolean canMoveTo(DistributionStatus next) {
        return switch (this) {
            case REVIEWING -> next == REVIEW_SUCCESS;
            case REVIEW_SUCCESS -> next == END;
            case END -> false;
        };
    }
}
