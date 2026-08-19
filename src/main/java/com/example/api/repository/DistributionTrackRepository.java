package com.example.api.repository;


import com.example.api.model.entity.DistributionTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionTrackRepository  extends JpaRepository<DistributionTrack, String> {
    List<DistributionTrack> findAllByDisId(String disId);
}
