package com.example.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.entity.SystemLog;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.enums.LogModule;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.LoginLogService;
import com.example.api.service.SystemLogService;
import com.example.api.utils.JwtTokenUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The two logs are paginated, and the page says how much more there is.
 *
 * <p>Both endpoints returned every row before S10. These tables grow by a row per audited request
 * and a row per login attempt, so "every row" is a response that gets slower for the whole life of
 * the deployment and is unusable long before it gets slow enough to notice.
 */
@WebMvcTest(LogController.class)
@Import({SecurityConfiguration.class, GlobalResponseHandler.class})
class LogControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;
    @MockitoBean LoginLogService loginLogService;
    @MockitoBean SystemLogService systemLogService;

    private static SystemLog entry() {
        SystemLog log = new SystemLog();
        log.setId("log-1");
        log.setAccount("demo@loggi.example");
        log.setModule(LogModule.COMMODITY);
        log.setBusinessType(BusinessType.QUERY);
        log.setIp("127.0.0.1");
        log.setMethod("controller.CommodityController.findAll");
        log.setCostMs(5L);
        log.setSuccess(true);
        log.setTime(LocalDateTime.of(2026, 8, 20, 10, 30, 0));
        return log;
    }

    @Test
    @WithMockUser
    @DisplayName("The response is a page: the items, and how many there are in total")
    void systemLog_isReturnedAsAPage() throws Exception {
        Page<SystemLog> page = new PageImpl<>(List.of(entry()), PageRequest.of(0, 20), 137);
        given(systemLogService.getAll(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/systemlog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("log-1"))
                .andExpect(jsonPath("$.data.items[0].businessType").value("QUERY"))
                // ISO-8601 since the per-field @JsonFormat came off; see
                // DistributionTrackContractTest for why.
                .andExpect(jsonPath("$.data.items[0].time").value("2026-08-20T10:30:00"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                // The number that makes the page usable. Without a total a client cannot tell
                // a last page from a page that happens to be short.
                .andExpect(jsonPath("$.data.totalItems").value(137))
                .andExpect(jsonPath("$.data.totalPages").value(7));
    }

    @Test
    @WithMockUser
    @DisplayName(
            "An unasked-for page is the newest twenty, because a log read backwards is not a log")
    void systemLog_defaultsToNewestFirst() throws Exception {
        given(systemLogService.getAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/systemlog")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(systemLogService).getAll(captor.capture());
        Pageable used = captor.getValue();
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort().getOrderFor("time"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @WithMockUser
    @DisplayName("page and size are honoured when asked for")
    void systemLog_acceptsPageAndSize() throws Exception {
        given(systemLogService.getAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 137));

        mockMvc.perform(get("/api/systemlog").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(systemLogService).getAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }
}
