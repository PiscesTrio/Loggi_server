package com.example.api.service.impl;


import com.example.api.model.entity.DistributionTrack;
import com.example.api.repository.DistributionTrackRepository;
import com.example.api.service.DistributionTrackService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
@Service
public class DistributionTrackServiceImpl implements DistributionTrackService {
    @Resource
    private DistributionTrackRepository distributionTrackRepository;

    @Override
    public List<DistributionTrack> findByDisId(String disId){
        return distributionTrackRepository.findAllByDisId(disId);
    }

    @Override
    public DistributionTrack save(DistributionTrack distributionTrack) {
        distributionTrack.setTime(LocalDateTime.now());
        return distributionTrackRepository.save(distributionTrack);
    }



}
