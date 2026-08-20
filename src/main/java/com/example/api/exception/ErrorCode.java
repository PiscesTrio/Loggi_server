package com.example.api.exception;

/**
 * What went wrong, as something a client can branch on.
 *
 * <p>Until now the only machine-readable part of a failure was the HTTP status, and 409 covers both
 * "that driver is already out" and "that stock movement would go negative" — two things a client
 * wants to say very differently. Everything else was in {@code msg}, in Chinese, written for
 * whoever happened to be reading the response that day.
 *
 * <p>That is the same mistake V9 and V10 fixed in the database, one layer up: a natural-language
 * string doing a value's job. It survived longer here because a message *is* meant to be read — the
 * point is that it is meant to be read by a person whose language the server does not know.
 *
 * <p>Each constant carries the status it answers with, so the two cannot drift apart. {@code msg}
 * stays alongside: it is what the server logs, and what a client too old to know these codes still
 * shows.
 */
public enum ErrorCode {

    // --- not found ---------------------------------------------------------------------
    WAREHOUSE_NOT_FOUND(404),
    DRIVER_NOT_FOUND(404),
    VEHICLE_NOT_FOUND(404),
    DISTRIBUTION_NOT_FOUND(404),
    COMMODITY_NOT_IN_WAREHOUSE(404),
    COMMODITY_NOT_FOUND(404),
    NOT_FOUND(404),

    // --- conflicts ---------------------------------------------------------------------
    DRIVER_UNAVAILABLE(409),
    VEHICLE_UNAVAILABLE(409),
    INSUFFICIENT_STOCK(409),
    ALREADY_EXISTS(409),

    // --- bad requests ------------------------------------------------------------------
    QUANTITY_NOT_POSITIVE(400),
    VALIDATION_FAILED(400),
    MALFORMED_REQUEST(400),
    BAD_REQUEST(400),

    // --- sign-in codes -----------------------------------------------------------------
    CODE_REQUESTED_TOO_SOON(429),
    CODE_ATTEMPTS_EXHAUSTED(429),
    CODE_DELIVERY_FAILED(502),

    // --- credentials -------------------------------------------------------------------
    // Two, because they are different situations for the caller: one has not signed in, the
    // other has and the credential no longer works. Both are 401, which is why the status
    // alone could not tell them apart.
    AUTHENTICATION_REQUIRED(401),
    TOKEN_INVALID(401),

    // --- everything else ---------------------------------------------------------------
    ACCESS_DENIED(403),
    METHOD_NOT_ALLOWED(405),
    INTERNAL_ERROR(500);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
