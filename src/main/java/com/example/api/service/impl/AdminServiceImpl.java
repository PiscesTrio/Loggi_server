package com.example.api.service.impl;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.model.enums.Role;
import com.example.api.repository.AdminRepository;
import com.example.api.service.AdminService;
import com.example.api.service.EmailService;
import com.example.api.utils.JwtTokenUtil;
import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Resource private AdminRepository adminRepository;

    @Resource private EmailService emailService;

    @Resource private JwtTokenUtil jwtTokenUtil;

    @Resource private PasswordEncoder passwordEncoder;

    @Override
    public Admin save(Admin admin) throws Exception {
        if (admin.getEmail().length() < 8 || admin.getPassword().length() < 5)
            throw new Exception("请求参数异常");
        // Encode before anything can persist it. The length check above runs on the
        // password as typed, which is the only point at which that is meaningful.
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    @Override
    public Admin findById(String id) {
        return adminRepository.findById(id).orElse(null);
    }

    @Override
    public void sendEmail(String email) {
        Admin admin = adminRepository.findAdminByEmail(email);
        if (admin == null) {
            // Deliberately silent. This threw "不存在的邮箱账户" from an endpoint that needs
            // no authentication, so anyone could ask it which addresses have accounts and
            // get a straight answer - the same enumeration leak loginByPassword goes out of
            // its way to avoid a few lines below. The caller sees the identical reply either
            // way; the difference is only whether an e-mail is actually sent.
            log.debug("Verification code requested for an address with no account");
            return;
        }
        emailService.sendVerificationCode(email);
    }

    @Override
    public Admin loginByPassword(LoginDto dto) throws Exception {
        Admin one = adminRepository.findAdminByEmail(dto.getEmail());
        // One message for both branches on purpose: distinguishing "no such account"
        // from "wrong password" tells an attacker which addresses are registered.
        if (one == null || !passwordEncoder.matches(dto.getPassword(), one.getPassword())) {
            throw new Exception("邮箱或密码错误");
        }
        return one;
    }

    @Override
    public Admin loginByEmail(LoginDto dto) throws Exception {
        boolean status = emailService.checkVerificationCode(dto.getEmail(), dto.getCode());
        if (!status) throw new Exception("验证码错误");
        return adminRepository.findAdminByEmail(dto.getEmail());
    }

    @Override
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Override
    public String createToken(Admin admin, long exp) {
        // No parsing left to do. This used to split a semicolon-joined string, which meant
        // deciding what null, "" and ";" each meant - three spellings of "no roles" that the
        // column allowed and the code had to guess at.
        List<String> roles =
                admin.getRoles() == null
                        ? List.of()
                        : admin.getRoles().stream().map(Role::getValue).toList();
        return jwtTokenUtil.createToken(admin.getEmail(), roles, exp);
    }

    @Override
    public void delete(String id) {
        adminRepository.deleteById(id);
    }
}
