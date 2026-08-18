package com.example.api.controller;

import com.example.api.model.entity.Admin;
import com.example.api.model.enums.Role;
import com.example.api.repository.AdminRepository;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.AdminService;
import com.example.api.service.LoginLogService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    // SecurityConfiguration's filter chain needs one. These cases send no Authorization header,
    // so the mock's null resolveToken() is exactly the "no token" path; what the filter does
    // with a real token is JwtAuthorizationFilterTest's subject.
    @MockitoBean JwtTokenUtil jwtTokenUtil;

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
    @DisplayName("findAll without a token is refused at the URL layer with 401")
    void findAll_withoutAuth_returns401() throws Exception {
        // S00 pinned this as "HTTP 200 carrying code=403 in the body" and said in its own
        // comment that the test would go red on purpose once a real status came back.
        // It has: authorizeHttpRequests now refuses the request before any handler runs,
        // and the entry point answers 401 because no credential was presented.
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = {"NOBODY"})
    @DisplayName("An authenticated caller with the wrong role still gets HTTP 200 carrying code=403 "
            + "(pins the remaining swallowed-status bug)")
    void findAll_authenticatedButWrongRole_stillReturns200WithCode403InBody() throws Exception {
        // The other half of what S00 pinned is still here. Once the request is
        // authenticated it reaches the handler, @PreAuthorize denies it, and
        // GlobalExceptionHandler returns a BODY rather than a status: there is no
        // @ResponseStatus and no ResponseEntity anywhere in src/main, so the HTTP status
        // stays 200 and the 403 exists only as a JSON field.
        //
        // S03 fixed the unauthenticated case by refusing it earlier. This case belongs to
        // the exception-handling slice, and this assertion is what keeps it visible until
        // then - deleting the old test outright would have lost it.
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.msg").value("你没有访问权限"));
    }

    @Test
    @DisplayName("login accepts an explicit null for the primitive boolean field")
    void login_withExplicitNullRemember_isAccepted() throws Exception {
        // The Flutter client's LoginDto.toJson() always emits every key, so `remember`
        // arrives as an explicit null rather than being absent. Jackson 2 mapped that
        // to false; Jackson 3 flipped FAIL_ON_NULL_FOR_PRIMITIVES to true and rejects
        // it with 400 before the controller is ever reached.
        //
        // An omitted field still works, which is why probing the endpoint with curl
        // did not reveal this — only the real client did.
        when(adminService.loginByPassword(any())).thenReturn(new Admin());
        when(adminService.createToken(any(), anyLong())).thenReturn("stub.jwt.token");

        mockMvc.perform(post("/api/admin/login?type=password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.c\",\"password\":\"p\",\"code\":null,\"remember\":null}"))
                .andExpect(status().isOk())
                // Wrapped by GlobalResponseHandler, so the token sits under data.
                .andExpect(jsonPath("$.data.token").exists());
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
