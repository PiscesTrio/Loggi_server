package com.example.api.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import com.example.api.model.enums.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column(length = 100, nullable = false)
    private String email;

    // 30 characters could not hold a hash: BCrypt produces 60, and the {bcrypt} prefix
    // the delegating encoder writes brings it to 68. Storing plaintext is what made the
    // old width look sufficient.
    @Column(length = 100, nullable = false)
    // Accepted on the way in, never sent on the way out. The entity is serialised
    // straight back by /login, /init and the admin list, so before this the login
    // response handed the caller the stored password - plaintext until this slice,
    // and a bcrypt hash after it. Neither belongs in a response body: a hash is
    // still a credential-equivalent to anyone willing to spend GPU time on it.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * The roles granted to this administrator.
     *
     * <p>Was a single String holding them semicolon-joined, parsed with {@code split(";")}
     * wherever anyone needed them. That representation cannot be queried — "who has
     * ROLE_ADMIN" is a LIKE over a text column that also matches ROLE_ADMIN_SOMETHING — it
     * cannot be constrained, so a typo'd role name was stored as happily as a real one, and
     * it made the empty case ambiguous: null, "" and ";" all mean no roles.
     *
     * <p>An element collection instead: a join table with a foreign key back to the admin
     * and the role stored as its enum name. EAGER because authentication needs them on every
     * request and there is at most a handful per administrator — the one place where lazy
     * loading would buy a second query for nothing.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_roles",
            joinColumns = @JoinColumn(name = "admin_id"),
            foreignKey = @ForeignKey(name = "fk_admin_roles_admin"))
    @Column(name = "role", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new LinkedHashSet<>();

}
