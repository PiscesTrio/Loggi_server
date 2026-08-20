package com.example.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.controller.AdminController;
import com.example.api.controller.CommodityController;
import com.example.api.controller.WarehouseController;
import com.example.api.model.entity.Admin;
import com.example.api.model.enums.Role;
import com.example.api.repository.AdminRepository;
import com.example.api.service.AdminService;
import com.example.api.service.CommodityService;
import com.example.api.service.LoginLogService;
import com.example.api.service.WarehouseService;
import com.example.api.utils.JwtTokenUtil;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What an unauthenticated request may reach.
 *
 * <p>Until this slice the answer was "everything": the filter chain ended in {@code
 * anyRequest().permitAll()}, so URL-level authorization did not exist and the only thing standing
 * between an anonymous caller and the data was whatever {@code @PreAuthorize} a controller happened
 * to carry. Seven of the thirteen controllers carry none, and WarehouseController's was commented
 * out.
 *
 * <p>These cases are the matrix itself, not a sample of it. A permit list is exactly the kind of
 * configuration where a wrong path silently opens or closes a door — the mistake produces a working
 * application either way, and only an assertion of the intended answer catches it.
 */
@WebMvcTest({AdminController.class, CommodityController.class, WarehouseController.class})
@Import(SecurityConfiguration.class)
class AuthorizationMatrixTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminService adminService;
    @MockitoBean AdminRepository adminRepository;
    @MockitoBean LoginLogService loginLogService;
    @MockitoBean CommodityService commodityService;
    @MockitoBean WarehouseService warehouseService;
    // No Authorization header is sent by these cases, so the mock's null resolveToken
    // is exactly the anonymous path.
    @MockitoBean JwtTokenUtil jwtTokenUtil;

    /**
     * The login and init cases need the service to return an administrator.
     *
     * <p>They did not before S10, when the response was a Map that held whatever it was given and
     * serialised a null admin without complaint. The response is a view type now, built from the
     * entity, so an unstubbed mock returning null becomes a NullPointerException and a 500 - and
     * these tests assert who may reach an endpoint, not what it answers, so a 500 tells them
     * nothing they are asking about.
     */
    @BeforeEach
    void authenticationSucceeds() throws Exception {
        Admin admin = new Admin();
        admin.setId("admin-1");
        admin.setEmail("a@b.c");
        admin.setRoles(Set.of(Role.ROLE_SUPER_ADMIN));
        lenient().when(adminService.loginByPassword(any())).thenReturn(admin);
        lenient().when(adminService.loginByEmail(any())).thenReturn(admin);
        lenient().when(adminService.save(any())).thenReturn(admin);
        lenient().when(adminService.createToken(any(), anyLong())).thenReturn("a-token");
    }

    @Test
    @DisplayName("Password login is reachable without a token — it is where tokens come from")
    void loginByPassword_isPublic() throws Exception {
        mockMvc.perform(
                        post("/api/admin/login/password")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"a@b.c\",\"password\":\"p\",\"code\":null,\"remember\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("E-mail login is reachable without a token, and is now a separate route")
    void loginByEmail_isPublic() throws Exception {
        // The single /login endpoint took a bare `type` string with no @RequestParam, so
        // omitting it reached type.equals("email") on a null. Two routes, no parameter to
        // forget, and each can be rate-limited and documented on its own terms.
        mockMvc.perform(
                        post("/api/admin/login/email")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"a@b.c\",\"password\":null,\"code\":\"000000\",\"remember\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
            "hasInit is reachable without a token — a fresh install has no account to authenticate as")
    void hasInit_isPublic() throws Exception {
        mockMvc.perform(get("/api/admin/hasInit")).andExpect(status().isOk());
    }

    @Test
    @DisplayName(
            "init is reachable without a token; the guard against abuse is the initialised check, not authentication")
    void init_isPublic() throws Exception {
        // Any 2xx, not a specific one. This case asks whether an anonymous caller may reach
        // the endpoint; which success code it answers with is a different question, decided
        // in S10 (201, because it creates something) and asserted where that belongs.
        mockMvc.perform(
                        post("/api/admin/init")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"admin@loggi.example\",\"password\":\"password\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Requesting a verification code is reachable without a token, and is a POST")
    void verificationCode_isPublic() throws Exception {
        // Was GET /api/admin/sendEmail. Sending mail and writing server state is not a safe
        // method: GET is retried by proxies, prefetched by browsers, and logged with its
        // query string - which here is somebody's e-mail address.
        mockMvc.perform(post("/api/admin/verification-code?email=a@b.c"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The old GET sendEmail route is gone rather than merely unused")
    void oldSendEmailRoute_isNoLongerRoutable() throws Exception {
        // 401, not 404: an unmapped path is refused by the default-deny rule before the
        // dispatcher looks for a handler. Either way it is unreachable, which is the point.
        mockMvc.perform(get("/api/admin/sendEmail?email=a@b.c"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Business data is refused without a token: commodity")
    void commodity_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/commodity")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Business data is refused without a token: warehouse")
    void warehouse_requiresAuthentication() throws Exception {
        // Its class-level @PreAuthorize was commented out, so before this slice an
        // anonymous GET returned the warehouse list.
        mockMvc.perform(get("/api/warehouse")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The admin list is refused without a token")
    void adminList_requiresAuthentication() throws Exception {
        // Previously this reached the handler and came back HTTP 200 carrying code=403
        // in the body, because @PreAuthorize denied after the request had been let in.
        // Now it never reaches the handler.
        mockMvc.perform(get("/api/admin")).andExpect(status().isUnauthorized());
    }
}
