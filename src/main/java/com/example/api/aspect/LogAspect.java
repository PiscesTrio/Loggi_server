package com.example.api.aspect;

import com.example.api.annotation.Log;
import com.example.api.model.entity.SystemLog;
import com.example.api.service.SystemLogService;
import com.example.api.utils.IpUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {
    @Autowired
    private SystemLogService logService;

    /*
        Pointcut declaration
     */
    @Pointcut("@annotation(com.example.api.annotation.Log)")
    public void pt(){}

    /*
       Around advice
     */
    @Around("pt()")
    public Object Around(ProceedingJoinPoint point) throws Throwable {
        //record the start time
        long beginTime = System.currentTimeMillis();
        Object res = null;
        try {
            //invoke the target method
            res = point.proceed();
        }finally {
            //compute the elapsed time
            long time = System.currentTimeMillis() - beginTime;
            recordLog(point);
        }
        return res;
    }

    private void recordLog(ProceedingJoinPoint point){
        //get the current request
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        //get the target method signature
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Log annotation = method.getAnnotation(Log.class);
        //build the audit log record
        SystemLog systemLog = new SystemLog();
        systemLog.setModule(annotation.moudle());
        systemLog.setBusincessType(annotation.type().getName());
        systemLog.setIp(IpUtil.getIpAddr(request));
        systemLog.setTime(LocalDateTime.now());
        //get the fully-qualified method path
        systemLog.setMethod((signature.getDeclaringTypeName()+"."+signature.getName()).substring(16));
        systemLog.setAccount(currentAccount());
        //persist to the database
        logService.record(systemLog);
    }

    /**
     * Reads the caller from the security context rather than re-parsing the Authorization header.
     *
     * <p>By the time an advised method runs, JwtAuthorizationFilter has already verified the
     * token and stored the result. Parsing it a second time here would repeat the signature
     * check, duplicate the header format in a second place, and let an audit-logging concern
     * throw on a request that already succeeded.
     *
     * @return the authenticated account, or {@code null} for an unauthenticated request
     */
    private String currentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
