package com.example.api.service;

import com.example.api.model.entity.DistributionStatus;

import java.util.List;

public interface DistributionStatusService {
    List<DistributionStatus> findByDisId(String disId);

    DistributionStatus save(DistributionStatus distributionStatus);

    List<DistributionStatus> findAllByStatusAndDisId(Integer status,String disId);


}
