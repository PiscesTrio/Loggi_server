package com.example.api.model.vo;

import com.example.api.model.entity.Admin;
import com.example.api.model.enums.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * An administrator, as the API describes one.
 *
 * <p>What it leaves out is the reason it exists. The entity was serialised straight back
 * from three endpoints, and it carries {@code password} - held off the wire only by a
 * {@code @JsonProperty(WRITE_ONLY)} on the field. That annotation works, and it is also a
 * single line standing between a credential and every response: delete it, or add a field
 * without thinking, and the leak is back with nothing to catch it. A view type inverts the
 * default, so a field reaches a client because someone put it here.
 */
public record AdminVo(
        String id,
        String email,
        List<String> roles,
        LocalDateTime createAt) {

    public static AdminVo from(Admin admin) {
        Set<Role> roles = admin.getRoles();
        return new AdminVo(
                admin.getId(),
                admin.getEmail(),
                roles == null ? List.of() : roles.stream().map(Role::getValue).toList(),
                admin.getCreateAt());
    }
}
