package com.example.api.model.vo;

import com.example.api.model.entity.LoginLog;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * One login attempt, successful or not.
 */
public record LoginLogVo(
        String id,
        String email,
        Integer status,
        String ip,
        String browser,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
        Date date) {

    public static LoginLogVo from(LoginLog log) {
        return new LoginLogVo(log.getId(), log.getEmail(), log.getStatus(),
                log.getIp(), log.getBrowser(), log.getDate());
    }
}
