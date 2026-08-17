package com.example.api.handler;

import com.example.api.exception.AccountAndPasswordError;
import com.example.api.model.support.ResponseResult;
import com.example.api.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 捕获controller异常
 * controller抛出异常执行下边的函数
 * 返回Response写入ApiResult
 */
@ResponseBody
@RestControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private LoginLogService loginLogService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(value = Exception.class)
    public Object handleException(Exception e) {
        // 必须是 instanceof 而不是 getClass().equals()：新版 Spring Security 的 @PreAuthorize
        // 拒绝时抛的是 AccessDeniedException 的**子类** AuthorizationDeniedException，
        // 精确类比较会漏掉它，于是「无权限」会从 403 静默变成 400（实测 S01 升级后触发）。
        if (e instanceof AccessDeniedException) {
            return new ResponseResult<>(403, "你没有访问权限");
        }
        logger.warn(e.getMessage());
        return new ResponseResult<>(400, e.getMessage());
    }

}
