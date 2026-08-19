package com.example.api.model.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Inventory
 */
@Data
@ToString(exclude = {"warehouse", "commodity"})
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class Inventory extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    /**
     * The warehouse holding this stock, and the commodity it is stock of.
     *
     * <p>Both were bare strings. They really did hold ids — unlike {@code distribution.wid},
     * which held a name — but with no foreign key behind them, so a stock row could name a
     * warehouse that had been deleted and nothing would say so.
     */
    // Serialised as the id alone, not as a nested object. These columns were plain id
    // strings before this slice (disId, wid, cid), so emitting an id keeps the client seeing
    // what it saw; inlining the row instead would nest a Distribution — with its own three
    // associations — inside every track point. Distribution's own driver/vehicle/warehouse
    // are treated the other way for the same reason: the client used to receive a copied
    // name and plate there, so it gets the real rows.
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id")
    private Commodity commodity;

    //commodity name
    private String name;

    //storage location / area
    private String location;

    //quantity
    private Integer count;

}
