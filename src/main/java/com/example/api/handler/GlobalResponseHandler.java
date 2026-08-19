package com.example.api.handler;

import com.example.api.annotation.DisableBaseResponse;
import com.example.api.model.support.ResponseResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import tools.jackson.databind.ObjectMapper;

/**
 * Wraps a controller's return value in the {@code {code,status,msg,data}} envelope.
 */
@ControllerAdvice(value = "com.example.api.controller")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * {@code hasMethodAnnotation}, not {@code hasParameterAnnotation}.
     *
     * <p>{@link DisableBaseResponse} targets METHOD, and the MethodParameter handed to a
     * ResponseBodyAdvice describes the <em>return value</em>, whose parameter index is -1
     * and whose parameter annotations are therefore always empty. The check could never
     * match, so the annotation did nothing wherever it was applied — the sort of defect
     * that leaves no trace, because an annotation that is silently ignored looks exactly
     * like one that is being honoured until you inspect the body.
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.hasMethodAnnotation(DisableBaseResponse.class)) {
            return false;
        }
        // Already an envelope: wrapping again would nest it inside its own data field.
        return !ResponseResult.class.isAssignableFrom(returnType.getParameterType());
    }

    private static int statusOf(ServerHttpResponse response) {
        return response instanceof ServletServerHttpResponse servlet
                ? servlet.getServletResponse().getStatus()
                : HttpStatus.OK.value();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        int status = statusOf(response);

        // 204 means no content, and that is not a suggestion: writing an envelope with a
        // No Content status produces a response that contradicts its own status line, and
        // some clients discard the body while others do not. Nothing is written.
        if (status == HttpStatus.NO_CONTENT.value()) {
            return null;
        }

        // The envelope's code follows the real status rather than always saying 200. S06
        // made the status line tell the truth about failures; a 201 arriving with code 200
        // in the body would reintroduce the same disagreement on the success path.
        ResponseResult<Object> wrapped =
                body == null ? new ResponseResult<>() : new ResponseResult<>(body);
        wrapped.setCode(status);

        // A String return type makes Spring pick StringHttpMessageConverter before this
        // advice runs, and that converter can only write a String — handing it a
        // ResponseResult threw ClassCastException at write time rather than failing to
        // compile. Serialising here produces the same envelope through the converter that
        // was already chosen.
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(wrapped);
        }
        return wrapped;
    }
}
