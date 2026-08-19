package com.example.api.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
@AllArgsConstructor

public class Admin extends Auditable {

    @Id
    @EqualsAndHashCode.Include
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

}
