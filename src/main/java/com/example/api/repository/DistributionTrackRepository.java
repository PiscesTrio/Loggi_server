package com.example.api.repository;

import com.example.api.model.entity.DistributionTrack;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionTrackRepository extends JpaRepository<DistributionTrack, String> {
    List<DistributionTrack> findAllByDistributionId(String distributionId);
}
