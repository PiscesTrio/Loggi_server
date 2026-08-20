package com.example.api.exception;

/**
 * A failure the caller can do something about, carrying the status that says so.
 *
 * <p>Unchecked on purpose. The services previously declared {@code throws Exception} and threw
 * {@code new Exception("...")}, which forces every caller to catch a type that says nothing, and
 * loses the distinction between "you asked for something impossible" and "the server broke". The
 * status travels with the exception so the handler does not have to guess it from a message.
 */
public class BizException extends RuntimeException {

    private final int status;

    public BizException(int status, String message) {
        super(message);
        this.status = status;
    }

    public BizException(String message) {
        this(400, message);
    }

    public int getStatus() {
        return status;
    }
}
