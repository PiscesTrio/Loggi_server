package com.example.api.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Administrator
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor

public class Admin {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(columnDefinition = "varchar(30) default 'LTD' not null")
    private String email;

    // 30 characters could not hold a hash: BCrypt produces 60, and the {bcrypt} prefix
    // the delegating encoder writes brings it to 68. Storing plaintext is what made the
    // old width look sufficient.
    @Column(columnDefinition = "varchar(100) default 'LTD' not null")
    // Accepted on the way in, never sent on the way out. The entity is serialised
    // straight back by /login, /init and the admin list, so before this the login
    // response handed the caller the stored password - plaintext until this slice,
    // and a bcrypt hash after it. Neither belongs in a response body: a hash is
    // still a credential-equivalent to anyone willing to spend GPU time on it.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String roles;

    private String createAt;

}
