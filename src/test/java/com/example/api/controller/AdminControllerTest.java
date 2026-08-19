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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(adminRepository.existsAdminByRolesContains(Role.ROLE_SUPER_ADMIN)).thenReturn(false);
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
    @DisplayName("An authenticated caller with the wrong role is refused with HTTP 403")
    void findAll_authenticatedButWrongRole_returns403() throws Exception {
        // S00 pinned this as "HTTP 200 carrying code=403 in the body" and said the
        // exception-handling slice would turn it red. It has. @PreAuthorize still denies
        // the request inside the handler, but GlobalExceptionHandler now answers with a
        // ResponseEntity, so the refusal reaches the client on the status line where every
        // HTTP client already looks, instead of only as a JSON field none of them read.
        //
        // The body is unchanged, which is the point: clients that were parsing $.code keep
        // working while clients that check the status finally see the truth.
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isForbidden())
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

        mockMvc.perform(post("/api/admin/login/password")
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

    @Test
    @DisplayName("A login without an e-mail is refused as a bad request, naming the field")
    void login_withBlankEmail_isRejectedWithTheFieldMessage() throws Exception {
        // Before S10 nothing checked this. A blank address reached the repository, missed,
        // and came back as "wrong e-mail or password" - the same answer a real address with
        // a wrong password gets. A caller who simply left the field out was told their
        // credentials were bad.
        mockMvc.perform(post("/api/admin/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"whatever\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("邮箱不能为空"));
    }

    @Test
    @DisplayName("A malformed e-mail is refused before authentication is attempted")
    void login_withMalformedEmail_neverReachesTheService() throws Exception {
        mockMvc.perform(post("/api/admin/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-address\",\"password\":\"whatever\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("邮箱格式不正确"));

        // The point of validating at the boundary: the service is not consulted at all, so a
        // request that could never succeed costs no password hash and no login-log row.
        verify(adminService, never()).loginByPassword(any());
    }

    @Test
    @DisplayName("The login response carries the administrator without the credential")
    void login_responseOmitsThePassword() throws Exception {
        Admin admin = new Admin();
        admin.setId("admin-1");
        admin.setEmail("demo@loggi.example");
        admin.setPassword("{bcrypt}$2a$10$whatever");
        admin.setRoles(java.util.Set.of(Role.ROLE_SUPER_ADMIN));
        when(adminService.loginByPassword(any())).thenReturn(admin);
        when(adminService.createToken(any(), anyLong())).thenReturn("a-token");

        mockMvc.perform(post("/api/admin/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@loggi.example\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("a-token"))
                .andExpect(jsonPath("$.data.admin.email").value("demo@loggi.example"))
                .andExpect(jsonPath("$.data.admin.roles[0]").value("ROLE_SUPER_ADMIN"))
                // Asserted rather than assumed. The entity used to be returned directly, and
                // what kept the hash off the wire was one annotation on one field; a view type
                // means a field is present because someone declared it.
                .andExpect(jsonPath("$.data.admin.password").doesNotExist());
    }
}
