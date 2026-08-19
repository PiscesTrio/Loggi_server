package com.example.api.controller;

import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.entity.DistributionTrack;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.DistributionService;
import com.example.api.service.DistributionTrackService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The delivery-track endpoints, asserted at the wire.
 *
 * <p>This slice renamed the entity behind them from {@code DistributionStatus} to
 * {@link DistributionTrack}, which in JPA also renames its table. A rename is only safe if
 * nothing outside the JVM can tell, and the app depends on these two endpoints for the
 * order timeline — so the contract is pinned here rather than assumed: same paths, same
 * JSON keys, same date format, in both directions.
 *
 * <p>Written against the renamed code, so it cannot prove the rename introduced no change
 * by itself. What it does is make the contract explicit from now on, and it was run against
 * the pre-rename tree first (with the old type name) to confirm it passed there too.
 */
@WebMvcTest(DistributionController.class)
@Import({SecurityConfiguration.class, GlobalResponseHandler.class})
class DistributionTrackContractTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;
    @MockitoBean DistributionService distributionService;
    @MockitoBean DistributionTrackService distributionTrackService;
    @MockitoBean DriverRepository driverRepository;
    @MockitoBean VehicleRepository vehicleRepository;

    private static DistributionTrack sample() {
        DistributionTrack track = new DistributionTrack();
        track.setId("track-1");
        track.setDisId("dis-1");
        track.setLat(35.672);
        track.setLng(139.817);
        track.setLocation("東京江東倉庫");
        track.setStatus(0);
        track.setTime(LocalDateTime.of(2026, 8, 19, 10, 30, 0));
        return track;
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/distribution/status keeps every JSON key the app reads")
    void getStatus_keepsItsJsonKeys() throws Exception {
        given(distributionTrackService.findByDisId("dis-1")).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/distribution/status").param("dis", "dis-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("track-1"))
                .andExpect(jsonPath("$.data[0].disId").value("dis-1"))
                .andExpect(jsonPath("$.data[0].lat").value(35.672))
                .andExpect(jsonPath("$.data[0].lng").value(139.817))
                .andExpect(jsonPath("$.data[0].location").value("東京江東倉庫"))
                .andExpect(jsonPath("$.data[0].status").value(0))
                // The app parses this string; a shape change here breaks the timeline.
                .andExpect(jsonPath("$.data[0].time").value("2026-08-19 10:30:00"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/distribution/status still binds the same request body")
    void saveStatus_bindsTheSameRequestBody() throws Exception {
        given(distributionTrackService.save(any(DistributionTrack.class))).willReturn(sample());

        mockMvc.perform(post("/api/distribution/status")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disId":"dis-1","lat":35.672,"lng":139.817,
                                 "location":"東京江東倉庫","status":0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.disId").value("dis-1"));

        ArgumentCaptor<DistributionTrack> captor = ArgumentCaptor.forClass(DistributionTrack.class);
        verify(distributionTrackService).save(captor.capture());
        DistributionTrack bound = captor.getValue();
        assertThat(bound.getDisId()).isEqualTo("dis-1");
        assertThat(bound.getLat()).isEqualTo(35.672);
        assertThat(bound.getLng()).isEqualTo(139.817);
        assertThat(bound.getLocation()).isEqualTo("東京江東倉庫");
        assertThat(bound.getStatus()).isZero();
    }
}
