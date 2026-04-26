package com.example.api.repository;


import com.example.api.model.entity.DistributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistributionStatusRepository  extends JpaRepository<DistributionStatus, String> {
    List<DistributionStatus> findAllByDisId(String disId);
    List<DistributionStatus> findAllByStatusAndDisId(Integer status,String disId);
}
