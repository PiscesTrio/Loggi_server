package com.example.api.handler;

import com.example.api.annotation.DisableBaseResponse;
import com.example.api.model.support.ResponseResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Intercepts the return value of every controller method and
 * wraps it in a ResponseResult<T> envelope.
 */
@ControllerAdvice(value = "com.example.api.controller")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(MethodParameter methodParameter, Class c) {
        return !methodParameter.hasParameterAnnotation(DisableBaseResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass,
                                  ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        return o == null ? new ResponseResult<>() : new ResponseResult<>(o);
    }

}
