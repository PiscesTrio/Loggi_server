package com.example.api.service.impl;


import com.example.api.model.entity.DistributionStatus;
import com.example.api.repository.DistributionStatusRepository;
import com.example.api.service.DistributionStatusService;
import com.example.api.utils.DataTimeUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
@Service
public class DistributionStatusServiceImpl implements DistributionStatusService {
    @Resource
    private DistributionStatusRepository distributionStatusRepository;

    @Override
    public List<DistributionStatus> findByDisId(String disId){
        return distributionStatusRepository.findAllByDisId(disId);
    }

    @Override
    public DistributionStatus save(DistributionStatus distributionStatus) {
        distributionStatus.setTime(LocalDateTime.now());
        return distributionStatusRepository.save(distributionStatus);
    }

    @Override
    public List<DistributionStatus> findAllByStatusAndDisId(Integer status,String disId) {
        return distributionStatusRepository.findAllByStatusAndDisId(status,disId);
    }


}
