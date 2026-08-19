package com.example.api.service;

import com.example.api.model.entity.DistributionTrack;

import java.util.List;

public interface DistributionTrackService {
    List<DistributionTrack> findByDisId(String disId);

    DistributionTrack save(DistributionTrack distributionTrack);


}
