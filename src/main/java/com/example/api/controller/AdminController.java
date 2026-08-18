package com.example.api.controller;

import com.example.api.exception.AccountAndPasswordError;
import com.example.api.exception.BizException;
import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.model.entity.LoginLog;
import com.example.api.model.enums.Role;
import com.example.api.model.support.ResponseResult;
import com.example.api.repository.AdminRepository;
import com.example.api.service.AdminService;
import com.example.api.service.LoginLogService;
import com.example.api.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    //logger instance
    Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Resource
    private AdminService adminService;

    @Resource
    private AdminRepository adminRepository;

    @Resource
    private LoginLogService loginLogService;

    @GetMapping("hasInit")
    public boolean hasInit() {
        return adminRepository.existsAdminByRoles(Role.ROLE_SUPER_ADMIN.getValue());
    }

    /**
     * Creates the first super administrator, once.
     *
     * <p>Deliberately anonymous: on a fresh install there is no account to authenticate
     * as, so requiring a token would make the system impossible to bootstrap. What stops
     * it being an open door is the check below — until this slice there was none, and any
     * caller could POST here at any time and mint themselves a super administrator on a
     * running system.
     */
    @PostMapping("/init")
    public Admin init(@RequestBody Admin admin) throws Exception {
        if (adminRepository.existsAdminByRoles(Role.ROLE_SUPER_ADMIN.getValue())) {
            throw new Exception("系统已初始化");
        }
        admin.setRoles(Role.ROLE_SUPER_ADMIN.getValue());
        return adminService.save(admin);
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_ADMIN')")
    public List<Admin> findAll() {
        return adminService.findAll();
    }

    @DeleteMapping("")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_ADMIN')")
    public void delete(String id) {
        adminService.delete(id);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_ADMIN')")
    public Admin save(@RequestBody Admin admin) throws Exception {
        return adminService.save(admin);
    }

    /**
     * Password login.
     *
     * <p>Two endpoints now, where there was one taking a {@code type} string. That string
     * had no {@code @RequestParam}, so a caller who omitted it reached
     * {@code type.equals("email")} on a null and the NPE was caught and reported as a wrong
     * password — control flow by exception, and a diagnosis that pointed at the user.
     */
    @PostMapping("/login/password")
    public Map<String, Object> loginByPassword(@RequestBody LoginDto dto, HttpServletRequest request) throws Exception {
        return login(dto, request, () -> adminService.loginByPassword(dto));
    }

    /** E-mail code login. Step two; step one is POST /verification-code. */
    @PostMapping("/login/email")
    public Map<String, Object> loginByEmail(@RequestBody LoginDto dto, HttpServletRequest request) throws Exception {
        return login(dto, request, () -> adminService.loginByEmail(dto));
    }

    /**
     * The part both share: issue a token, and record the attempt either way.
     *
     * <p>The old body wrapped everything in {@code catch (Exception e) { throw new
     * Exception("邮箱或密码错误"); }}. That is the right answer for a credential mismatch and
     * the wrong one for everything else it also caught: a rate-limit refusal, a mail server
     * outage, a locked account, a null pointer. The caller was told to check their password
     * while the actual reason was discarded — including the reasons they could have acted
     * on. BizException already carries a status and a message written to be shown, so it
     * passes through; anything else still collapses, because an unexpected failure during
     * authentication is the one case where saying less is right.
     */
    private Map<String, Object> login(LoginDto dto, HttpServletRequest request, AuthAttempt attempt) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Admin admin = null;
        String token = null;
        try {
            admin = attempt.authenticate();
            token = adminService.createToken(admin,
                    dto.isRemember() ? JwtTokenUtil.REMEMBER_EXPIRATION_TIME : JwtTokenUtil.EXPIRATION_TIME);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Login refused for {}", dto.getEmail(), e);
            throw new Exception("邮箱或密码错误");
        } finally {
            loginLogService.recordLog(dto, admin, request);
        }
        map.put("admin", admin);
        map.put("token", token);
        return map;
    }

    @FunctionalInterface
    private interface AuthAttempt {
        Admin authenticate() throws Exception;
    }

    /**
     * Issues a one-time code to an address.
     *
     * <p>POST, not the GET it was. A request that sends mail and writes server state is not
     * safe to retry, prefetch, or log with its parameters, which is what GET invites.
     *
     * <p>The reply is the same whether or not the address has an account. The old one threw
     * "不存在的邮箱账户", turning a public endpoint into an oracle for which addresses are
     * registered — the exact leak {@code loginByPassword} takes care to avoid one method
     * away.
     */
    @PostMapping("/verification-code")
    public ResponseResult<Void> sendVerificationCode(@RequestParam String email) {
        adminService.sendEmail(email);
        ResponseResult<Void> res = new ResponseResult<>();
        res.setMsg("如果该邮箱已注册，验证码已发送");
        res.setStatus(true);
        return res;
    }

}
