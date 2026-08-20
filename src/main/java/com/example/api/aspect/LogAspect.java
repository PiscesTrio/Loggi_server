package com.example.api.aspect;

import com.example.api.annotation.Log;
import com.example.api.model.entity.SystemLog;
import com.example.api.service.SystemLogService;
import com.example.api.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Writes an audit record for every method annotated with {@link Log}. */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** Stripped from the stored method name; it is the same on every row. */
    private static final String BASE_PACKAGE = "com.example.api.";

    @Autowired private SystemLogService logService;

    @Pointcut("@annotation(com.example.api.annotation.Log)")
    public void pt() {}

    /**
     * Runs the advised method, then records what happened to it.
     *
     * <p>Two things were wrong with doing this in a bare {@code finally}. The elapsed time was
     * measured and assigned to a local nobody read — the entity had no column for it — so every
     * record said only that the operation had occurred, never how long it took or whether it
     * worked. And an exception thrown while recording would have propagated out of the finally
     * block, taking the in-flight exception or the successful return with it: an audit-logging
     * failure would have become the caller's failure, and would have erased the very error it
     * existed to record.
     *
     * <p>So the outcome is captured rather than assumed, and recording cannot throw. If the audit
     * write fails the request still succeeds and the failure goes to the application log, which is
     * the only ordering that makes sense — the operation already happened, and refusing to write it
     * down does not undo it.
     */
    @Around("pt()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        boolean success = false;
        try {
            Object res = point.proceed();
            success = true;
            return res;
        } finally {
            recordLog(point, System.currentTimeMillis() - beginTime, success);
        }
    }

    private void recordLog(ProceedingJoinPoint point, long costMs, boolean success) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            Log annotation = method.getAnnotation(Log.class);

            SystemLog systemLog = new SystemLog();
            systemLog.setModule(annotation.module());
            systemLog.setBusinessType(annotation.type());
            systemLog.setIp(currentIp());
            systemLog.setTime(LocalDateTime.now());
            systemLog.setMethod(shortName(signature));
            systemLog.setAccount(currentAccount());
            systemLog.setCostMs(costMs);
            systemLog.setSuccess(success);

            logService.record(systemLog);
        } catch (Exception e) {
            log.error("Failed to write an audit record; the request itself was unaffected", e);
        }
    }

    /**
     * The advised method, without the package prefix every row shares.
     *
     * <p>This was {@code substring(16)} — a count of the characters in "com.example.api.". Correct,
     * and silently wrong the moment anything moves: a shorter package name would take characters
     * off the class name instead, and a qualified name shorter than 16 characters would throw
     * StringIndexOutOfBoundsException from inside the audit logger.
     */
    private String shortName(MethodSignature signature) {
        String qualified = signature.getDeclaringTypeName() + "." + signature.getName();
        return qualified.startsWith(BASE_PACKAGE)
                ? qualified.substring(BASE_PACKAGE.length())
                : qualified;
    }

    /**
     * The caller's IP, or null when there is no HTTP request behind this call.
     *
     * <p>{@code getRequestAttributes()} returns null off a request thread — a scheduled task or an
     * async call reaching an annotated method — and the old code dereferenced it on the next line.
     */
    private String currentIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return IpUtil.getIpAddr(request);
    }

    /**
     * Reads the caller from the security context rather than re-parsing the Authorization header.
     *
     * <p>By the time an advised method runs, JwtAuthorizationFilter has already verified the token
     * and stored the result. Parsing it a second time here would repeat the signature check,
     * duplicate the header format in a second place, and let an audit-logging concern throw on a
     * request that already succeeded.
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
