package com.example.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.DistributionTrack;
import com.example.api.model.enums.DistributionStatus;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.DistributionService;
import com.example.api.service.DistributionTrackService;
import com.example.api.utils.JwtTokenUtil;
import java.time.LocalDateTime;
import java.util.List;
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

/**
 * The delivery-track endpoints, asserted at the wire.
 *
 * <p>This slice renamed the entity behind them from {@code DistributionStatus} to {@link
 * DistributionTrack}, which in JPA also renames its table. A rename is only safe if nothing outside
 * the JVM can tell, and the app depends on these two endpoints for the order timeline — so the
 * contract is pinned here rather than assumed: same paths, same JSON keys, same date format, in
 * both directions.
 *
 * <p>Written against the renamed code, so it cannot prove the rename introduced no change by
 * itself. What it does is make the contract explicit from now on, and it was run against the
 * pre-rename tree first (with the old type name) to confirm it passed there too.
 *
 * <p><b>S09 changed this contract deliberately</b>, and this test is where that is recorded: {@code
 * disId} is now {@code distribution} (the same id, behind a real foreign key) and {@code status} is
 * the enum name rather than 0/1/2. The client has not been updated — the frontend realignment is
 * its own slice — so these assertions are the written form of what it will have to change to. See
 * docs/contract-changes.md.
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
        track.setDistribution(order("dis-1"));
        track.setLat(35.672);
        track.setLng(139.817);
        track.setLocation("東京江東倉庫");
        track.setStatus(DistributionStatus.REVIEWING);
        track.setTime(LocalDateTime.of(2026, 8, 19, 10, 30, 0));
        return track;
    }

    private static Distribution order(String id) {
        Distribution d = new Distribution();
        d.setId(id);
        return d;
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
                // disId in S07, `distribution` in S09, `distributionId` since S10. The column
                // was a bare string, became a real association, and is now a field on a view
                // type - which is the shape it should have had all along, and the only one of
                // the three that reads and writes the same way.
                .andExpect(jsonPath("$.data[0].distributionId").value("dis-1"))
                .andExpect(jsonPath("$.data[0].lat").value(35.672))
                .andExpect(jsonPath("$.data[0].lng").value(139.817))
                .andExpect(jsonPath("$.data[0].location").value("東京江東倉庫"))
                .andExpect(jsonPath("$.data[0].status").value("REVIEWING"))
                // ISO-8601, with the T. It was "2026-08-19 10:30:00" until the per-field
                // @JsonFormat came off: `format: date-time` in the OpenAPI document means
                // RFC 3339, and the space-separated form the server used to send was a
                // format the document had no way to express. Dart's DateTime.parse accepts
                // both, which is why reading never noticed; writing answered 400.
                .andExpect(jsonPath("$.data[0].time").value("2026-08-19T10:30:00"));
    }

    @Test
    @WithMockUser
    // The asymmetry is gone. Until S10 the response carried the parent as a bare id while a
    // request had to send it as an object, because @JsonIdentityReference can write an id
    // but cannot read a lone one back. A view type is not an entity and has no such
    // constraint: one field, one shape, both directions. This test is where that is checked.
    @DisplayName(
            "POST /api/distribution/status takes the parent id in the same shape it returns it")
    void saveStatus_bindsTheSameRequestBody() throws Exception {
        given(distributionTrackService.save(any(DistributionTrack.class))).willReturn(sample());

        mockMvc.perform(
                        post("/api/distribution/status")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"distributionId":"dis-1","lat":35.672,"lng":139.817,
                                 "location":"東京江東倉庫","status":"REVIEWING"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distributionId").value("dis-1"));

        ArgumentCaptor<DistributionTrack> captor = ArgumentCaptor.forClass(DistributionTrack.class);
        verify(distributionTrackService).save(captor.capture());
        DistributionTrack bound = captor.getValue();
        assertThat(bound.getDistribution().getId()).isEqualTo("dis-1");
        assertThat(bound.getLat()).isEqualTo(35.672);
        assertThat(bound.getLng()).isEqualTo(139.817);
        assertThat(bound.getLocation()).isEqualTo("東京江東倉庫");
        assertThat(bound.getStatus()).isEqualTo(DistributionStatus.REVIEWING);
    }
}
