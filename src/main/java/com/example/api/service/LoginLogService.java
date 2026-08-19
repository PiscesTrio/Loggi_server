package com.example.api.service;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.api.model.entity.LoginLog;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface LoginLogService {
    Page<LoginLog> getAll(Pageable pageable);
    void recordLog(LoginDto loginDto, Admin admin, HttpServletRequest request);
    void delLoginLog(String id);
}
