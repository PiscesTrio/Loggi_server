package com.example.api.service.impl;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.api.model.entity.LoginLog;
import com.example.api.repository.LoginLogRepository;
import com.example.api.service.LoginLogService;
import com.example.api.utils.BrowserUtil;
import com.example.api.utils.IpUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginLogServiceImpl implements LoginLogService {
    @Resource
    private LoginLogRepository loginLogRepository;

    /**
     * One page of the log, newest first.
     *
     * <p>It returned every row. That is fine on a demo database with a few dozen and
     * indefensible on anything that has been running: this table grows by one row per login
     * attempt, forever, and the endpoint would eventually load every one of them into memory to
     * serialise them into a response no client can use. Ordering is part of the contract
     * here, because "the first page" is meaningless without it.
     */
    @Override
    public Page<LoginLog> getAll(Pageable pageable) {
        return loginLogRepository.findAll(pageable);
    }

    @Override
    public void recordLog(LoginDto loginDto, Admin admin, HttpServletRequest request) {
        //create the log record
        LoginLog loginLog = new LoginLog();
        loginLog.setDate(LocalDateTime.now());
        loginLog.setEmail(loginDto.getEmail());
        //resolve the browser version
        loginLog.setBrowser(BrowserUtil.getBrower(request));
        loginLog.setIp(IpUtil.getIpAddr(request));
        if (admin == null){
            loginLog.setStatus(0);
        }else {
            loginLog.setStatus(1);
        }
        //write the log record to the database
        loginLogRepository.save(loginLog);
    }

    @Override
    public void delLoginLog(String id) {
        loginLogRepository.deleteById(id);
    }
}
