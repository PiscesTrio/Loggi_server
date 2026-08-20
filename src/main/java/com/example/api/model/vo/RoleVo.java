package com.example.api.model.vo;

import com.example.api.model.enums.Role;

/**
 * What {@code GET /api/role} hands out: the roles an administrator can be granted.
 *
 * <p>The endpoint used to return {@code Role[]} — the domain enum itself, serialised by a
 * {@code @JsonFormat(shape = OBJECT)} sitting on the enum. That annotation is there purely to shape
 * an HTTP response, which puts a wire-format decision inside the type the whole domain switches on:
 * change what the API should expose and you are editing an enum that security rules depend on.
 * Worse, it exposes whatever fields the enum happens to grow.
 *
 * <p>A separate view type lets the enum be an enum.
 *
 * <p>It carried a second key, {@code description}, holding Chinese display text. That key is gone:
 * a role's label belongs to whichever locale the reader is in, and the server does not know which
 * that is. A record rather than a bare {@code List<String>} because the endpoint has room to grow —
 * whether the caller may grant a given role, for instance — and that is a property of the role, not
 * a second parallel array.
 */
public record RoleVo(String value) {

    public static RoleVo of(Role role) {
        return new RoleVo(role.getValue());
    }
}
