package com.example.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

/** Sale */
@Data
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class Sale extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String company;

    private String number;

    private String commodity;

    /**
     * A quantity, declared as text. It could not be summed, ordered or range-queried, and the
     * column accepted "3個", "" and "-" alike. No backend code reads it, which is how it survived
     * this long.
     */
    private Integer count;

    /**
     * Money, so BigDecimal. IEEE 754 binary floating point cannot represent 0.10 exactly, and the
     * error compounds across additions — a total that is off by a cent is a bug nobody can defend.
     * Nothing in this codebase sums prices yet, which is precisely why this is cheap to fix now
     * rather than after something does.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    private String phone;

    private String description;

    private boolean pay;
}
