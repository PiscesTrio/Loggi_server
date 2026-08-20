package com.example.api.exception;

/**
 * A failure the caller can do something about, carrying the status that says so.
 *
 * <p>Unchecked on purpose. The services previously declared {@code throws Exception} and threw
 * {@code new Exception("...")}, which forces every caller to catch a type that says nothing, and
 * loses the distinction between "you asked for something impossible" and "the server broke". The
 * status travels with the exception so the handler does not have to guess it from a message.
 *
 * <p>It now carries an {@link ErrorCode} as well, and the status comes from that rather than being
 * passed separately — one place decides that a missing warehouse is a 404, instead of every throw
 * site remembering to agree.
 *
 * <p>The message stays, and stays Chinese. It is what gets logged, and what a client that does not
 * know the code still has to show. What changed is that it is no longer the only thing a client has
 * to work with: {@code msg} is for a reader who is already here, {@code errorCode} is for one whose
 * language the server has no way of knowing.
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getStatus() {
        return errorCode.getStatus();
    }
}
