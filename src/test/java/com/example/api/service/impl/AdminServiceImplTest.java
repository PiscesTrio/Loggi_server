package com.example.api.service.impl;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.repository.AdminRepository;
import com.example.api.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Characterization tests pinning the CURRENT real behavior of AdminServiceImpl,
 * including its known bugs. This slice fixes nothing; it only freezes today's
 * behavior into a red/green safety net.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock AdminRepository adminRepository;
    @Mock EmailService emailService;
    @InjectMocks AdminServiceImpl adminService;

    @Test
    @DisplayName("loginByPassword passes the raw password straight to the query (pins current plaintext behavior)")
    void loginByPassword_passesRawPasswordToQuery() throws Exception {
        // Current impl runs a derived query with the plaintext password
        // (a bug, but that is the behavior today — to be fixed in the BCrypt slice).
        LoginDto dto = new LoginDto();
        dto.setEmail("admin@logi.com");
        dto.setPassword("plaintext123");
        Admin found = new Admin();
        found.setEmail("admin@logi.com");
        when(adminRepository.findAdminByEmailAndPassword("admin@logi.com", "plaintext123"))
                .thenReturn(found);

        Admin result = adminService.loginByPassword(dto);

        assertThat(result).isSameAs(found);
        // Pin it: the raw password is used directly, with no encode/matches.
        verify(adminRepository).findAdminByEmailAndPassword("admin@logi.com", "plaintext123");
    }

    @Test
    @DisplayName("loginByPassword throws a generic Exception when no admin matches")
    void loginByPassword_whenNotFound_throwsGenericException() {
        when(adminRepository.findAdminByEmailAndPassword(anyString(), anyString())).thenReturn(null);
        LoginDto dto = new LoginDto();
        dto.setEmail("x@y.com");
        dto.setPassword("p");

        // Pin it: a raw java.lang.Exception is thrown, not a custom exception.
        assertThatThrownBy(() -> adminService.loginByPassword(dto))
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("邮箱或密码错误");
    }

    @Test
    @DisplayName("save rejects email length < 8 or password length < 5 (pins weak-validation bug)")
    void save_whenEmailUnder8OrPasswordUnder5_throws() {
        Admin admin = new Admin();
        admin.setEmail("a@b.com"); // length 7 < 8
        admin.setPassword("123456");
        assertThatThrownBy(() -> adminService.save(admin))
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("请求参数异常");
        verifyNoInteractions(adminRepository); // not persisted when validation fails
    }

    @Test
    @DisplayName("save accepts boundary lengths (email=8, password=5), persists, and sets createAt")
    void save_atBoundaryLengths_persistsAndSetsCreateAt() throws Exception {
        Admin admin = new Admin();
        admin.setEmail("12345678");   // length exactly 8 (not < 8)
        admin.setPassword("12345");   // length exactly 5 (not < 5)
        when(adminRepository.save(admin)).thenReturn(admin);

        adminService.save(admin);

        verify(adminRepository).save(admin);
        assertThat(admin.getCreateAt()).isNotNull(); // save() sets createAt internally
    }
}
