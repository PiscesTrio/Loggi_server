package com.example.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.handler.GlobalExceptionHandler;
import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.entity.Distribution;
import com.example.api.model.enums.DistributionStatus;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.DistributionService;
import com.example.api.service.DistributionTrackService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code PATCH /api/distribution/{id}/status}: who may move an order, and what a refusal looks like
 * on the wire.
 *
 * <p>Approving takes a driver and a truck off the road for everyone else, so it is an
 * administrator's call. Until this endpoint existed the same effect was reachable by any signed-in
 * account, by filing an order whose status already said approved or finished.
 */
@WebMvcTest(DistributionController.class)
@Import({SecurityConfiguration.class, GlobalResponseHandler.class, GlobalExceptionHandler.class})
class DistributionStatusEndpointTest {

    private static final String BODY = "{\"status\":\"REVIEW_SUCCESS\"}";

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;
    @MockitoBean DistributionService distributionService;
    @MockitoBean DistributionTrackService distributionTrackService;
    @MockitoBean DriverRepository driverRepository;
    @MockitoBean VehicleRepository vehicleRepository;

    private static Distribution moved() {
        Distribution d = new Distribution();
        d.setId("o1");
        d.setStatus(DistributionStatus.REVIEW_SUCCESS);
        return d;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("An administrator moves the order named in the path, and gets it back")
    void admin_movesTheOrder() throws Exception {
        given(distributionService.changeStatus("o1", DistributionStatus.REVIEW_SUCCESS))
                .willReturn(moved());

        mockMvc.perform(
                        patch("/api/distribution/o1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("o1"))
                .andExpect(jsonPath("$.data.status").value("REVIEW_SUCCESS"));
        verify(distributionService).changeStatus("o1", DistributionStatus.REVIEW_SUCCESS);
    }

    @Test
    @WithMockUser(roles = "SALE")
    @DisplayName("A signed-in account without an admin role is refused before anything moves")
    void nonAdmin_isRefused() throws Exception {
        mockMvc.perform(
                        patch("/api/distribution/o1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        verifyNoInteractions(distributionService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("An illegal move is a 409 carrying INVALID_STATUS_TRANSITION")
    void illegalMove_is409WithItsCode() throws Exception {
        given(distributionService.changeStatus(any(), any()))
                .willThrow(
                        new BizException(
                                ErrorCode.INVALID_STATUS_TRANSITION,
                                "an order in END cannot move to REVIEW_SUCCESS"));

        mockMvc.perform(
                        patch("/api/distribution/o1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("A request without a target status is a 400, not a move to null")
    void missingStatus_is400() throws Exception {
        mockMvc.perform(
                        patch("/api/distribution/o1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        verifyNoInteractions(distributionService);
    }
}
