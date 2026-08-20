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
 * <p>A separate view type lets the enum be an enum. The JSON is unchanged — the same two keys the
 * {@code @JsonFormat} produced — so this is a change of ownership, not of contract.
 */
public record RoleVo(String value, String description) {

    public static RoleVo of(Role role) {
        return new RoleVo(role.getValue(), role.getDescription());
    }
}
