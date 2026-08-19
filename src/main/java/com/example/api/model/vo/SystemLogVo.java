package com.example.api.model.vo;

import com.example.api.model.entity.SystemLog;
import com.example.api.model.enums.BusinessType;

import java.time.LocalDateTime;

/**
 * One audited operation.
 *
 * <p>{@code businessType} is the enum name since S09; it used to be the Chinese label,
 * which put a UI language into stored data. Rendering it belongs to whoever is displaying
 * it.
 */
public record SystemLogVo(
        String id,
        String account,
        String module,
        BusinessType businessType,
        String ip,
        String method,
        Long costMs,
        boolean success,
        LocalDateTime time) {

    public static SystemLogVo from(SystemLog log) {
        return new SystemLogVo(log.getId(), log.getAccount(), log.getModule(),
                log.getBusinessType(), log.getIp(), log.getMethod(),
                log.getCostMs(), log.isSuccess(), log.getTime());
    }
}
