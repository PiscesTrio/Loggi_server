package com.example.api.model.enums;

/**
 * The kind of operation an audited method performs, as written into the operation log.
 *
 * <p>Was spelled {@code BusincessType}, with a field {@code busincessType} on {@link
 * com.example.api.model.entity.SystemLog} to match. The misspelling reached the annotation on every
 * audited endpoint and the JSON the log screen reads.
 *
 * <p>Each constant also carried a Chinese label, reachable through {@code getName()}. S10 removed
 * the {@code toString()} that returned it — Jackson 3 serialises enums through {@code toString()}
 * where Jackson 2 used {@code name()}, so the audit table held INSERT while the API answered 新增 —
 * and after that nothing read the label at all. The client has mapped the value to display text
 * since. The field is gone rather than kept "in case": a second, unread copy of a name is exactly
 * the shape of thing that drifts from the one that is read.
 */
public enum BusinessType {
    OTHER,
    QUERY,
    INSERT,
    UPDATE,
    DELETE,
    EXPORT,
    FORCE
}
