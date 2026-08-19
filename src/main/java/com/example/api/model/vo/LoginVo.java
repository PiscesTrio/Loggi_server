package com.example.api.model.vo;

import com.example.api.model.entity.Admin;

/**
 * What a successful login answers with.
 *
 * <p>It was a {@code Map<String, Object>} assembled in the controller, holding "admin" and
 * "token". A map has no shape: nothing declares those two keys, nothing stops a third being
 * added, and the OpenAPI document generated from it can only say "an object". The client's
 * own model was written by reading the code that built it.
 *
 * <p>The admin is an {@link AdminVo}, so the password cannot travel even by accident - the
 * map held the entity.
 */
public record LoginVo(AdminVo admin, String token) {

    public static LoginVo of(Admin admin, String token) {
        return new LoginVo(AdminVo.from(admin), token);
    }
}
