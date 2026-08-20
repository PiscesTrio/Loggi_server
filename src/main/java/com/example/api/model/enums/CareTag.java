package com.example.api.model.enums;

/**
 * A handling instruction attached to a delivery.
 *
 * <p>The names follow ISO 780, the standard set of pictorial handling marks printed on shipping
 * packages, so that each constant already has an agreed meaning and an agreed symbol rather than
 * being a translation of the Chinese phrase it replaces.
 *
 * <p>What it replaces: {@code distribution.care}, one {@code varchar(255)} holding the selected
 * labels comma-joined <em>with a trailing comma</em>, because that is what the client's
 * multi-select produced. Every problem V7 described for {@code admin.roles} applied here too — it
 * could not be queried without a LIKE that matches neighbours, could not be constrained, and made
 * the empty case ambiguous between NULL, {@code ''} and {@code ','}.
 */
public enum CareTag {
    FRAGILE,
    KEEP_DRY,
    KEEP_AWAY_FROM_SUNLIGHT,
    PROTECT_FROM_HEAT,
    DO_NOT_ROLL,
    DO_NOT_STACK,
    REFRIGERATE,
    FLAMMABLE
}
