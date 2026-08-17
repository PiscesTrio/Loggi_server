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
 * Catches exceptions thrown by controllers and writes the result
 * into the ApiResult response envelope.
 */
@ResponseBody
@RestControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private LoginLogService loginLogService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(value = Exception.class)
    public Object handleException(Exception e) {
        // Must be instanceof, not getClass().equals(): recent Spring Security raises
        // AuthorizationDeniedException — a SUBCLASS of AccessDeniedException — when
        // @PreAuthorize denies. An exact-class comparison misses it, so "forbidden"
        // silently turns from 403 into 400 (observed after the S01 upgrade).
        if (e instanceof AccessDeniedException) {
            return new ResponseResult<>(403, "你没有访问权限");
        }
        logger.warn(e.getMessage());
        return new ResponseResult<>(400, e.getMessage());
    }

}
