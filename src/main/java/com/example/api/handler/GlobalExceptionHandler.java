package com.example.api.handler;

import com.example.api.exception.BizException;
import com.example.api.model.support.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Turns an exception into a response whose HTTP status says what happened.
 *
 * <p>The previous version returned an object rather than a {@link ResponseEntity}, so every
 * failure left as <b>HTTP 200</b> with a code buried in the body. No client branches on
 * that: Dio, fetch, curl and every other caller read the status line to decide whether a
 * request succeeded, and this handler told all of them it had. Three separate defects in
 * this project trace back to it — a login screen reporting every failure as a wrong
 * password, a home screen unwrapping a null after a failed request, and an authorization
 * denial that arrived looking like success.
 *
 * <p>It also flattened everything into 400. "You asked for something impossible", "that
 * record does not exist" and "the server broke" are different answers, and the caller can
 * act on only two of them.
 *
 * <p>The envelope is unchanged, so existing clients keep parsing one shape; what changed
 * is that the status line now agrees with it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // The LoginLogService field that used to sit here was never read. It was not merely
    // dead: being @Autowired(required=true) it made every test slice that did not mock
    // LoginLogService fail to start its context, with a message about an unsatisfied
    // dependency rather than about the test.

    /** A failure the caller can act on, carrying the status that says which one. */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResponseResult<Void>> handleBiz(BizException e) {
        log.debug("Business rule refused the request: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ResponseResult<>(e.getStatus(), e.getMessage()));
    }

    /**
     * Denied by {@code @PreAuthorize}.
     *
     * <p>Matched with {@code instanceof}, not {@code getClass().equals()}: current Spring
     * Security raises AuthorizationDeniedException, a subclass, so an exact comparison
     * misses it and "forbidden" silently became "bad request" — observed after S01.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseResult<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ResponseResult<>(403, "你没有访问权限"));
    }

    /** Optional.get() on an absent record; the inventory paths reach it through a missing id. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseResult<Void>> handleNotFound(NoSuchElementException e) {
        log.debug("Requested record does not exist: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseResult<>(404, "请求的资源不存在"));
    }

    /**
     * The checked {@code Exception} the services still throw by hand.
     *
     * <p>Kept while {@code throws Exception} remains in the service signatures. Its message
     * is written by us and is safe to return; it is the catch-all below that must not leak.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleChecked(Exception e) {
        if (e instanceof RuntimeException) {
            return handleUnexpected(e);
        }
        log.debug("Request refused: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ResponseResult<>(400, e.getMessage()));
    }

    /**
     * Anything else.
     *
     * <p>Logged with the throwable rather than {@code e.getMessage()}, which discarded the
     * stack and produced a literal {@code null} line for a NullPointerException — the one
     * case where the stack is the only information there is.
     *
     * <p>The caller gets a fixed message. The old handler returned {@code e.getMessage()}
     * for everything, which hands out internal detail for a failure nobody outside can act
     * on, and returns {@code msg: null} when the exception carries no message.
     */
    private ResponseEntity<ResponseResult<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception while serving a request", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseResult<>(500, "服务器内部错误"));
    }
}
