package com.example.api.model.entity;

import com.example.api.model.enums.BusinessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    // primary key
    private String id;

    // account
    private String account;
    // functional module
    private String module;

    // operation type
    /**
     * Stored as the enum name, not as its Chinese label.
     *
     * <p>LogAspect wrote {@code annotation.type().getName()} — the display text — so the audit
     * table held "查询" and a reader had to map it back. That bakes a UI language into stored data:
     * translating the interface would either strand every historical row or require rewriting them.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BusinessType businessType;

    // user IP
    @Column(length = 45, nullable = false)
    private String ip;

    // request method
    @Column(length = 200, nullable = false)
    private String method;

    // operation time
    private LocalDateTime time;

    // how long the operation took, in milliseconds
    // LogAspect measured this from the first version and had nowhere to put it
    private Long costMs;

    // whether the operation completed or threw
    // the aspect records from a finally block, so a failed operation is written too - it was
    // previously indistinguishable from one that worked
    @Column(columnDefinition = "boolean default true not null")
    private boolean success;
}
