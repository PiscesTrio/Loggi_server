package com.example.api.model.dto;

import com.example.api.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

/**
 * What a caller sends to create an administrator.
 *
 * <p>The password is write-only by construction here rather than by annotation: a request type has
 * no way to send one back, so there is no {@code @JsonProperty(WRITE_ONLY)} to forget to add.
 *
 * <p>A minimum length, because there was none. The column was widened in S09 to hold a bcrypt hash;
 * nothing has ever constrained what goes into it.
 */
@Data
public class AdminRequest {

    @NotBlank(message = "email is required")
    @Email(message = "email is not a valid address")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    /**
     * Ignored by {@code /init}, which always grants ROLE_SUPER_ADMIN - there is no other sensible
     * answer for the first account, and letting the request choose would let an anonymous caller
     * pick their own privileges on a fresh install.
     */
    private Set<Role> roles;
}
