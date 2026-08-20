package com.example.api.handler;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.support.ResponseResult;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns an exception into a response whose HTTP status says what happened.
 *
 * <p>The previous version returned an object rather than a {@link ResponseEntity}, so every failure
 * left as <b>HTTP 200</b> with a code buried in the body. No client branches on that: Dio, fetch,
 * curl and every other caller read the status line to decide whether a request succeeded, and this
 * handler told all of them it had. Three separate defects in this project trace back to it — a
 * login screen reporting every failure as a wrong password, a home screen unwrapping a null after a
 * failed request, and an authorization denial that arrived looking like success.
 *
 * <p>It also flattened everything into 400. "You asked for something impossible", "that record does
 * not exist" and "the server broke" are different answers, and the caller can act on only two of
 * them.
 *
 * <p>The envelope is unchanged, so existing clients keep parsing one shape; what changed is that
 * the status line now agrees with it.
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
                .body(new ResponseResult<>(e.getStatus(), e.getMessage(), e.getErrorCode()));
    }

    /**
     * Denied by {@code @PreAuthorize}.
     *
     * <p>Matched with {@code instanceof}, not {@code getClass().equals()}: current Spring Security
     * raises AuthorizationDeniedException, a subclass, so an exact comparison misses it and
     * "forbidden" silently became "bad request" — observed after S01.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseResult<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ResponseResult<>(403, "access denied", ErrorCode.ACCESS_DENIED));
    }

    /** Optional.get() on an absent record; the inventory paths reach it through a missing id. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseResult<Void>> handleNotFound(NoSuchElementException e) {
        log.debug("Requested record does not exist: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResponseResult<>(404, "not found", ErrorCode.NOT_FOUND));
    }

    /**
     * A request that failed its constraints, answered with the constraint that failed.
     *
     * <p>Without this it would reach the catch-all below, be recognised as an {@link
     * ErrorResponse}, and come back as a flat "the request could not be read". That is true and
     * useless: the DTO declares which field is wrong and why, and discarding that at the boundary
     * means the caller has to guess which of five fields the server disliked.
     *
     * <p>Only the first violation is returned. Validation order is not defined, so the set is not
     * stable between runs, and a caller fixing one field at a time gets a coherent conversation
     * either way. The remaining messages stay in the debug log.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValidationFailure(
            MethodArgumentNotValidException e) {
        String message =
                e.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .findFirst()
                        .orElse("the request could not be read");
        log.debug("Request rejected by validation: {}", e.getBindingResult().getAllErrors());
        return ResponseEntity.badRequest()
                .body(new ResponseResult<>(400, message, ErrorCode.VALIDATION_FAILED));
    }

    /**
     * A row the database refused: a duplicate where a unique constraint says there may be only one.
     *
     * <p>Added with the constraints themselves in S08, because a constraint with no handler is
     * worse than no constraint. Until V2 the uniqueness of an email, a commodity name or a number
     * plate was checked in Java — read, then write — which two concurrent requests pass
     * simultaneously. Moving the check into the database closes that race, but it also changes how
     * a duplicate arrives: no longer a value the service inspected, but a
     * DataIntegrityViolationException thrown from the flush. That is a RuntimeException, so without
     * this method it reached the catch-all and a user who typed a plate that already existed was
     * told the server had broken, with a stack trace filed against it.
     *
     * <p>409, because the request was well-formed and the caller can act on the answer: pick
     * another value. The message deliberately does not name the column — the constraint name is a
     * schema detail, and echoing the driver's text back would leak the table layout.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseResult<Void>> handleConstraintViolation(
            DataIntegrityViolationException e) {
        log.debug("Database rejected the write: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ResponseResult<>(409, "that already exists", ErrorCode.ALREADY_EXISTS));
    }

    /**
     * A request the server could not read: unparseable body, or a value that will not convert to
     * the parameter's type.
     *
     * <p>Listed by name because neither implements {@link ErrorResponse} - the branch below does
     * not catch them, and both are RuntimeExceptions, so they fell through to the 500. Telling a
     * caller the server broke when they sent malformed JSON sends them looking in the wrong place,
     * and it puts a stack trace in the server log for something that is not a server fault.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseResult<Void>> handleUnreadableRequest(Exception e) {
        log.debug("Request could not be read: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(
                        new ResponseResult<>(
                                400, "the request could not be read", ErrorCode.MALFORMED_REQUEST));
    }

    /**
     * Everything else, in the order the three cases have to be tested.
     *
     * <p>Spring's own MVC exceptions come first. They already carry the right status - an unmapped
     * path is a NoResourceFoundException that means 404, an unparseable body means 400, the wrong
     * verb means 405 - and a bare {@code @ExceptionHandler(Exception.class)} intercepts all of them
     * before Spring's default handling can say so. Measured against a running server rather than
     * reasoned about: an unknown path answered 400 while repeating "No static resource api/nope for
     * request '/api/nope'." back to the caller, and malformed JSON, an unsupported method and an
     * unconvertible parameter all answered 500. Three of those are the caller's mistake and none is
     * a server fault. Every one of them passed the unit tests, because the tests exercised the
     * handlers directly and never asked what Spring itself throws on the way in.
     *
     * <p>Testing {@code ErrorResponse} rather than listing exception classes covers the ones not
     * thought of, including any Spring adds later.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleFallback(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            return handleSpringMvc(e, errorResponse);
        }
        if (e instanceof RuntimeException) {
            return handleUnexpected(e);
        }
        // The checked Exception the services still throw by hand, while `throws Exception`
        // remains in their signatures. Its message is written by us and is safe to return.
        log.debug("Request refused: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ResponseResult<>(400, e.getMessage(), ErrorCode.BAD_REQUEST));
    }

    /**
     * A Spring MVC exception, answered with the status it already carries.
     *
     * <p>The message is ours, not {@code getBody().getDetail()}. Spring's detail names server-side
     * artefacts - "No static resource api/nope" tells a caller which internal lookup failed - and
     * it is written in English in an interface that is not.
     */
    private ResponseEntity<ResponseResult<Void>> handleSpringMvc(
            Exception e, ErrorResponse errorResponse) {
        int status = errorResponse.getStatusCode().value();
        // The pair, not two switches: a status whose message says "not found" and whose code
        // says BAD_REQUEST is worse than either alone, and two lists drift.
        var answer =
                switch (status) {
                    case 404 -> new Answer("not found", ErrorCode.NOT_FOUND);
                    case 405 -> new Answer("method not allowed", ErrorCode.METHOD_NOT_ALLOWED);
                    case 415 -> new Answer("unsupported media type", ErrorCode.MALFORMED_REQUEST);
                    default ->
                            errorResponse.getStatusCode().is4xxClientError()
                                    ? new Answer(
                                            "the request could not be read", ErrorCode.BAD_REQUEST)
                                    : new Answer("internal server error", ErrorCode.INTERNAL_ERROR);
                };
        if (errorResponse.getStatusCode().is5xxServerError()) {
            log.error("Spring MVC reported a server-side failure", e);
        } else {
            log.debug("Request rejected before the handler: {}", e.getMessage());
        }
        return ResponseEntity.status(status)
                .body(new ResponseResult<>(status, answer.msg(), answer.code()));
    }

    /**
     * Anything else.
     *
     * <p>Logged with the throwable rather than {@code e.getMessage()}, which discarded the stack
     * and produced a literal {@code null} line for a NullPointerException — the one case where the
     * stack is the only information there is.
     *
     * <p>The caller gets a fixed message. The old handler returned {@code e.getMessage()} for
     * everything, which hands out internal detail for a failure nobody outside can act on, and
     * returns {@code msg: null} when the exception carries no message.
     */
    private ResponseEntity<ResponseResult<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception while serving a request", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseResult<>(500, "internal server error", ErrorCode.INTERNAL_ERROR));
    }

    /** A status's message and its code, chosen together. */
    private record Answer(String msg, ErrorCode code) {}
}
