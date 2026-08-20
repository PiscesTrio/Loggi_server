package com.example.api.service;

import com.example.api.model.entity.Distribution;
import java.util.List;

public interface DistributionService {

    Distribution save(Distribution distribution);

    List<Distribution> findAll();
}
