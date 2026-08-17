package com.example.api.model.support;

import lombok.Data;

@Data
public class ResponseResult<T> {

    private Integer code;

    private boolean status;

    private String msg;

    private T data;

    public ResponseResult() {
        this.code = 200;
        this.status = true;
        this.msg = null;
        this.data = null;
    }

    //return data without a msg
    public ResponseResult(T data) {
        this.code = 200;
        this.status = true;
        this.msg = null;
        this.data = data;
    }

    //carries msg and data by default
    public ResponseResult(String msg, T data) {
        this.code = 200;
        this.status = true;
        this.msg = msg;
        this.data = data;
    }

    //default error response
    public ResponseResult(Integer code, String msg) {
        this.code = code;
        this.status = false;
        this.msg = msg;
        this.data = null;
    }

}
