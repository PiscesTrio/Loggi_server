package com.example.api.model.dto;

import com.example.api.model.enums.DistributionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * The status a caller wants an existing order moved to.
 *
 * <p>Only the target. The current status is the server's to read, and letting the caller name it
 * would let a stale client assert a transition from a state the order has already left.
 */
@Data
public class DistributionStatusChange {

    @NotNull(message = "status is required")
    private DistributionStatus status;
}
