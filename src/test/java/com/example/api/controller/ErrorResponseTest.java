package com.example.api.controller;

import com.example.api.annotation.DisableBaseResponse;
import com.example.api.exception.BizException;
import com.example.api.handler.GlobalResponseHandler;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP status has to mean something.
 *
 * <p>Every failure used to leave as HTTP 200 with a code in the body, because
 * GlobalExceptionHandler returned an object and nothing ever set a status. A client
 * cannot branch on that: Dio, retrofit, fetch and curl all decide success from the
 * status line, so "the server said no" arrived indistinguishable from "the server said
 * yes". It is the root of three separate defects already recorded in this project — the
 * login screen reporting every failure as a wrong password, the home screen unwrapping
 * a null after a failed request, and an authorization denial that looked like success.
 *
 * <p>Driven through a controller that exists only here, so the assertions are about the
 * handlers rather than about whichever real endpoint happens to throw today.
 *
 * <p>In this package on purpose. GlobalResponseHandler is a
 * {@code @ControllerAdvice("com.example.api.controller")}, so a fixture controller
 * anywhere else is never advised — the envelope assertions passed vacuously against
 * one in the handler package, which is the failure mode this comment exists to stop
 * anyone repeating.
 */
@WebMvcTest(ErrorResponseTest.ThrowingController.class)
// ThrowingController is imported, not merely named in @WebMvcTest's controllers
// attribute: that attribute is a component-scan FILTER, and a controller nested in a
// test class is never scanned, so the filter had nothing to keep. Every request came
// back as "No static resource ..." — a 404 dressed up as 400 by the old handler.
@Import({ErrorResponseTest.ThrowingController.class, SecurityConfiguration.class,
        GlobalResponseHandler.class})
class ErrorResponseTest {

    @RestController
    @RequestMapping("/api/test-errors")
    static class ThrowingController {

        @GetMapping("/biz")
        public String biz() {
            throw new BizException(409, "库存不足");
        }

        @GetMapping("/not-found")
        public String notFound() {
            throw new NoSuchElementException("no such commodity");
        }

        @GetMapping("/unexpected")
        public String unexpected() {
            throw new IllegalStateException("something the caller cannot act on");
        }

        @GetMapping("/ok")
        public String ok() {
            return "plain";
        }

        @GetMapping("/raw")
        @DisableBaseResponse
        public String raw() {
            return "raw";
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;

    @Test
    @WithMockUser
    @DisplayName("A business failure carries its own status, not 200")
    void bizException_usesItsOwnStatus() throws Exception {
        mockMvc.perform(get("/api/test-errors/biz"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.msg").value("库存不足"));
    }

    @Test
    @WithMockUser
    @DisplayName("A missing record is 404, not 400 and not 200")
    void missingRecord_is404() throws Exception {
        // NoSuchElementException is what Optional.get() throws, and the inventory paths
        // reach it whenever a commodity id does not exist.
        mockMvc.perform(get("/api/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("An unexpected failure is 500, and its message is not handed to the caller")
    void unexpected_is500AndSaysNothingSpecific() throws Exception {
        // The old handler put e.getMessage() straight into the body for everything. For a
        // failure the caller cannot act on that is a leak, and for a NullPointerException
        // the message is null, so the client received msg: null and no status to read.
        mockMvc.perform(get("/api/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("服务器内部错误"));
    }

    @Test
    @WithMockUser
    @DisplayName("A successful String response is still wrapped, without a ClassCastException")
    void stringBody_isWrapped() throws Exception {
        // Wrapping a String used to hand a ResponseResult to StringHttpMessageConverter,
        // which had already been selected for the String return type — a
        // ClassCastException at write time rather than a compile error.
        mockMvc.perform(get("/api/test-errors/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("plain"));
    }

    @Test
    @WithMockUser
    @DisplayName("@DisableBaseResponse actually disables the envelope")
    void disableBaseResponse_isHonoured() throws Exception {
        // The annotation existed, targeted METHOD, and was checked with
        // hasParameterAnnotation - which inspects parameters, and a return value has
        // none. It could never match, so the annotation was inert wherever it was used.
        mockMvc.perform(get("/api/test-errors/raw"))
                .andExpect(status().isOk())
                .andExpect(content().string("raw"));
    }
}
