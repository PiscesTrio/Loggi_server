package com.example.api.aspect;

import com.example.api.annotation.Log;
import com.example.api.model.entity.SystemLog;
import com.example.api.service.SystemLogService;
import com.example.api.utils.IpUtil;
import com.example.api.utils.JwtTokenUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
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
        //read the token and parse it to resolve the current account
        String token = request.getHeader(JwtTokenUtil.TOKEN_HEADER);
        systemLog.setAccount(JwtTokenUtil.getUsername(token));
        //persist to the database
        logService.record(systemLog);
    }
}
