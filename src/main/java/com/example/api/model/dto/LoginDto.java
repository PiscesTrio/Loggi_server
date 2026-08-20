package com.example.api.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request payload.
 *
 * <p>The constraints are the point. Before them nothing checked the address at all: a blank one
 * reached the repository, missed, and came back as "wrong e-mail or password" - the same answer a
 * real address with a wrong password gets, so a caller who simply forgot the field was told their
 * credentials were bad. A malformed request is now refused as a malformed request, before any
 * handler runs.
 *
 * <p>Password and code carry no constraint on purpose: exactly one of the two is required depending
 * on which endpoint is called, and expressing "one or the other" here would mean a class-level
 * validator that has to know which path it is on. The endpoints know, and each says so itself.
 */
@Data
public class LoginDto {

    @NotBlank(message = "email is required")
    @Email(message = "email is not a valid address")
    private String email;

    private String password;

    private String code;

    private boolean remember;
}
