package com.example.api.service;

import com.example.api.model.entity.Distribution;
import com.example.api.model.enums.DistributionStatus;
import java.util.List;

public interface DistributionService {

    /** Files a new order. It starts under review and dispatches nobody. */
    Distribution create(Distribution distribution);

    /**
     * Moves an existing order to {@code next}, taking or releasing its driver and vehicle as the
     * transition requires.
     */
    Distribution changeStatus(String id, DistributionStatus next);

    List<Distribution> findAll();
}
