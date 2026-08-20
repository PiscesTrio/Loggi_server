package com.example.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Creation and modification timestamps, filled by JPA auditing rather than by hand.
 *
 * <p>Every entity used to carry {@code private String createAt} and set it in its service with
 * {@code DataTimeUtil.getNowTimeString()}. Two problems came from that.
 *
 * <p>The type was wrong. A timestamp stored as text cannot be compared, ranged over or ordered by
 * the database without parsing every row, and nothing stopped a caller supplying "yesterday", "" or
 * a differently formatted string — the column accepted all of them.
 *
 * <p>And the assignment was a convention rather than a rule: the same line repeated in eight
 * services, which works exactly as long as everyone remembers. {@link Distribution}, the order this
 * whole system exists to move, never got the field at all — it records the delivery time the user
 * asked for and has never recorded when it was created.
 *
 * <p>Auditing removes the choice. The listener writes both fields on persist and update; a value
 * supplied by a caller is overwritten, which is the point — these describe what the database did,
 * not what the request claimed.
 *
 * <p>Column names stay {@code create_at} / {@code update_at}. They would read better as {@code
 * created_at}, but renaming them buys nothing here and costs a migration plus another change to a
 * JSON contract this slice already disturbs enough.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createAt;

    @LastModifiedDate
    @Column(name = "update_at")
    private LocalDateTime updateAt;
}
