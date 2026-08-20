package com.example.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.enums.Role;
import com.example.api.security.SecurityConfiguration;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /api/role} answers with a view type rather than the domain enum.
 *
 * <p>The point of the change is that the enum no longer decides the JSON, so the test that matters
 * is the one asserting the response carries exactly the one documented key — that is what stops a
 * field added to {@link Role} tomorrow from silently appearing on the wire.
 */
@WebMvcTest(RoleController.class)
@Import({SecurityConfiguration.class, GlobalResponseHandler.class})
class RoleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;

    @Test
    @WithMockUser
    @DisplayName("Lists every grantable role by its value")
    void listsGrantableRoles() throws Exception {
        mockMvc.perform(get("/api/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(Role.ROLES.length)))
                .andExpect(jsonPath("$.data[0].value").value(Role.ROLES[0].getValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("Exposes only value, so enum fields cannot leak onto the wire")
    void exposesNothingBeyondTheOneDocumentedKey() throws Exception {
        mockMvc.perform(get("/api/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*]", hasSize(Role.ROLES.length)))
                .andExpect(jsonPath("$.data[0].*", hasSize(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("Sends no display text, in any language")
    void sendsNoDisplayText() throws Exception {
        // The reason the description key is gone. Asserted rather than left to the key count
        // above because "one key" and "that key is an identifier" are different claims, and a
        // future field could satisfy the first while breaking the second.
        mockMvc.perform(get("/api/role"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data[?(@.value =~ /^ROLE_[A-Z_]+$/)]",
                                hasSize(Role.ROLES.length)));
    }

    @Test
    @WithMockUser
    @DisplayName("Never offers the super administrator role")
    void neverOffersSuperAdmin() throws Exception {
        // Role.ROLES omits it because there is no public path to granting it. Asserted here
        // because the omission is a security decision, not a formatting one, and the array
        // literal it lives in is easy to extend without noticing.
        mockMvc.perform(get("/api/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.value == 'ROLE_SUPER_ADMIN')]", hasSize(0)));
    }
}
