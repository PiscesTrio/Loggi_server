package com.example.api.controller;

import com.example.api.model.enums.Role;
import com.example.api.repository.AdminRepository;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.AdminService;
import com.example.api.service.LoginLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Characterization tests pinning routing, the {code,status,msg,data} response
 * envelope, and the security-filter behavior under the CURRENT implementation.
 *
 * Notes:
 *  - @WebMvcTest auto-registers @ControllerAdvice (GlobalResponseHandler /
 *    GlobalExceptionHandler), so do NOT @Import GlobalResponseHandler (that would
 *    duplicate the bean and trigger BeanDefinitionOverrideException).
 *  - SecurityConfiguration must be @Import-ed explicitly. Under Spring Boot 2.7 the
 *    @WebMvcTest slice picked it up on its own (it was a WebSecurityConfigurerAdapter,
 *    a type the slice filter includes). After S01 it is a plain @Configuration that
 *    merely DECLARES a SecurityFilterChain bean, which the slice filter does not match,
 *    so the slice fell back to auto-configured security (anyRequest().authenticated())
 *    and every unauthenticated request became 401. The @Import restores exactly the
 *    wiring these assertions were written against — the assertions themselves are
 *    unchanged.
 *
 * WARNING: this is the highest-risk item in S00 (it depends on the Spring
 * Web/Security slice context loading). On first run, execute
 * `mvn -Dtest=AdminControllerTest test`, observe the real status codes, and tighten
 * the assertions to match — the essence of a characterization test: observe first,
 * then freeze.
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfiguration.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AdminService adminService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean LoginLogService loginLogService;

    @Test
    @DisplayName("hasInit boolean is wrapped into the {code,status,msg,data} envelope")
    void hasInit_booleanIsWrappedIntoDataField() throws Exception {
        when(adminRepository.existsAdminByRoles(Role.ROLE_SUPER_ADMIN.getValue())).thenReturn(false);
        mockMvc.perform(get("/api/admin/hasInit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").value(false)); // pin the envelope shape
    }

    @Test
    @DisplayName("findAll without authentication returns HTTP 200 carrying code=403 in the body (pins the swallowed-status bug)")
    void findAll_withoutAuth_returnsHttp200WithCode403InBody() throws Exception {
        // Pin the CURRENT (broken) behavior, not the expected one:
        //   1. JwtAuthorizationFilter lets an empty token through (chain.doFilter).
        //   2. SecurityConfiguration never calls authorizeRequests(), so there is NO
        //      URL-level authorization; the request reaches the handler.
        //   3. @PreAuthorize on AdminController.findAll() denies the anonymous
        //      principal and raises AccessDeniedException.
        //   4. GlobalExceptionHandler catches it and RETURNS a body instead of a
        //      status: `new ResponseResult<>(403, "你没有访问权限")`. There is no
        //      @ResponseStatus and no ResponseEntity anywhere in src/main, so the
        //      HTTP status stays 200 and the 403 exists only as a JSON field.
        //   5. GlobalResponseHandler is @ControllerAdvice("com.example.api.controller")
        //      while GlobalExceptionHandler lives in ...api.handler, so the error body
        //      is NOT re-wrapped — the fields sit at the JSON root.
        // This is the bug S06 (全局异常/响应处理修复) will fix: when it starts returning
        // a real 403, THIS test goes red on purpose — update it then, deliberately.
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.msg").value("你没有访问权限"));
    }

    @Test
    @WithMockUser(roles = {"SUPER_ADMIN"})
    @DisplayName("findAll with ROLE_SUPER_ADMIN is allowed (200)")
    void findAll_withSuperAdminRole_returns200() throws Exception {
        // @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', ...)"); Spring adds the ROLE_ prefix.
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk());
    }
}
