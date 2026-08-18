package com.example.api.security;

import com.example.api.controller.AdminController;
import com.example.api.controller.CommodityController;
import com.example.api.controller.WarehouseController;
import com.example.api.repository.AdminRepository;
import com.example.api.service.AdminService;
import com.example.api.service.CommodityService;
import com.example.api.service.LoginLogService;
import com.example.api.service.WarehouseService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What an unauthenticated request may reach.
 *
 * <p>Until this slice the answer was "everything": the filter chain ended in
 * {@code anyRequest().permitAll()}, so URL-level authorization did not exist and the
 * only thing standing between an anonymous caller and the data was whatever
 * {@code @PreAuthorize} a controller happened to carry. Seven of the thirteen
 * controllers carry none, and WarehouseController's was commented out.
 *
 * <p>These cases are the matrix itself, not a sample of it. A permit list is exactly
 * the kind of configuration where a wrong path silently opens or closes a door — the
 * mistake produces a working application either way, and only an assertion of the
 * intended answer catches it.
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

    @Test
    @DisplayName("Login is reachable without a token — it is where tokens come from")
    void login_isPublic() throws Exception {
        mockMvc.perform(post("/api/admin/login?type=password")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.c\",\"password\":\"p\",\"code\":null,\"remember\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("hasInit is reachable without a token — a fresh install has no account to authenticate as")
    void hasInit_isPublic() throws Exception {
        mockMvc.perform(get("/api/admin/hasInit")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("init is reachable without a token; the guard against abuse is the initialised check, not authentication")
    void init_isPublic() throws Exception {
        mockMvc.perform(post("/api/admin/init")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@loggi.example\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sendEmail is reachable without a token — it serves the e-mail login path")
    void sendEmail_isPublic() throws Exception {
        mockMvc.perform(get("/api/admin/sendEmail?email=a@b.c")).andExpect(status().isOk());
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
