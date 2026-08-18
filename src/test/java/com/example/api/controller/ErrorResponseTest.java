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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        /** Only reachable with a parseable body and an int-shaped parameter. */
        @PostMapping("/echo")
        public String echo(@RequestBody Map<String, Object> body, @RequestParam int size) {
            return String.valueOf(size);
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

    // ---------------------------------------------------------------------------------
    // What Spring throws before a handler ever runs. These four went unnoticed through a
    // green unit suite and were found by curl against a running server, because every test
    // above enters through a controller that is reached - and these never reach one.
    // ---------------------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("An unmapped path is 404, and does not repeat the internal lookup back")
    void unmappedPath_is404WithoutLeakingTheResourceName() throws Exception {
        // Was 400 with "No static resource api/nope for request '/api/nope'." - the wrong
        // status, and a sentence describing this server's internals in a language the
        // interface does not use.
        mockMvc.perform(get("/api/test-errors/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("请求的资源不存在"));
    }

    @Test
    @WithMockUser
    @DisplayName("An unparseable body is 400, not 500")
    void malformedJson_is400() throws Exception {
        // HttpMessageNotReadableException is a RuntimeException, so the fallback sent it
        // to the 500 branch: the caller was told the server had broken when in fact they
        // had sent something the server could not read.
        mockMvc.perform(post("/api/test-errors/echo?size=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser
    @DisplayName("The wrong HTTP verb is 405, not 500")
    void wrongMethod_is405() throws Exception {
        mockMvc.perform(post("/api/test-errors/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.msg").value("不支持的请求方法"));
    }

    @Test
    @WithMockUser
    @DisplayName("A parameter that will not convert is 400, not 500")
    void unconvertibleParameter_is400() throws Exception {
        mockMvc.perform(post("/api/test-errors/echo?size=abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
