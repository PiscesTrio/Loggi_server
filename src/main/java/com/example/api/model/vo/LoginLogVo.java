package com.example.api.model.vo;

import com.example.api.model.entity.LoginLog;

import java.time.LocalDateTime;

/**
 * One login attempt, successful or not.
 */
public record LoginLogVo(
        String id,
        String email,
        Integer status,
        String ip,
        String browser,
        LocalDateTime date) {

    public static LoginLogVo from(LoginLog log) {
        return new LoginLogVo(log.getId(), log.getEmail(), log.getStatus(),
                log.getIp(), log.getBrowser(), log.getDate());
    }
}
