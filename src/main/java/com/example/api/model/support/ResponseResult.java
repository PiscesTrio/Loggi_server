package com.example.api.model.support;

import com.example.api.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ResponseResult<T> {

    private Integer code;

    private boolean status;

    private String msg;

    /**
     * What went wrong, for a client that has to say it in its own language.
     *
     * <p>Absent on success, and absent from the JSON when null rather than present as {@code
     * "errorCode": null} — a key that only appears when it means something is easier to read and
     * easier to branch on.
     *
     * <p>{@code msg} is still here beside it. The two are for different readers: {@code msg} is
     * Chinese prose for the log and for a client that does not know these codes; the code is for
     * one that does. Dropping {@code msg} would have been the tidier change and the wrong one — it
     * is what makes this backwards compatible.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorCode errorCode;

    private T data;

    public ResponseResult() {
        this.code = 200;
        this.status = true;
        this.msg = null;
        this.data = null;
    }

    // return data without a msg
    public ResponseResult(T data) {
        this.code = 200;
        this.status = true;
        this.msg = null;
        this.data = data;
    }

    // carries msg and data by default
    public ResponseResult(String msg, T data) {
        this.code = 200;
        this.status = true;
        this.msg = msg;
        this.data = data;
    }

    // default error response
    public ResponseResult(Integer code, String msg) {
        this(code, msg, null);
    }

    public ResponseResult(Integer code, String msg, ErrorCode errorCode) {
        this.code = code;
        this.status = false;
        this.msg = msg;
        this.errorCode = errorCode;
        this.data = null;
    }
}
