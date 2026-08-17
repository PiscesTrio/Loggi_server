package com.example.api.service.impl;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.model.entity.LoginLog;
import com.example.api.repository.LoginLogRepository;
import com.example.api.service.LoginLogService;
import com.example.api.utils.BrowserUtil;
import com.example.api.utils.IpUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class LoginLogServiceImpl implements LoginLogService {
    @Resource
    private LoginLogRepository loginLogRepository;

    @Override
    public List<LoginLog> getAll() {
        return loginLogRepository.findAll();
    }

    @Override
    public void recordLog(LoginDto loginDto, Admin admin, HttpServletRequest request) {
        //create the log record
        LoginLog loginLog = new LoginLog();
        loginLog.setDate(new Date());
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
