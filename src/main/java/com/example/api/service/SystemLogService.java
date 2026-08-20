package com.example.api.service;

import com.example.api.model.dto.SystemLogQuery;
import com.example.api.model.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SystemLogService {
    public void record(SystemLog log);

    Page<SystemLog> getAll(Pageable pageable);

    public void delete(String id);

    Page<SystemLog> query(SystemLogQuery filter, Pageable pageable);
}
