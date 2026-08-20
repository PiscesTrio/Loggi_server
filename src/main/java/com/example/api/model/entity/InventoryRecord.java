package com.example.api.model.entity;

import com.example.api.model.enums.InventoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

/** Inventory record: stock-out / stock-in movement */
@Data
@ToString(exclude = {"warehouse", "commodity"})
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class InventoryRecord extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    // The @JsonIdentityReference that used to sit here is gone with S10's view types. It
    // existed to make an association serialise as a bare id while the entity was itself the
    // wire format - a workaround for a problem the boundary removed, and it could only ever
    // work in one direction: it wrote an id and could not read a lone one back.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id")
    private Commodity commodity;

    // commodity name
    private String name;

    private Integer count;

    /**
     * Which way the stock moved. Was an Integer holding +1 or -1, with the two constants private to
     * InventoryRecordServiceImpl.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private InventoryType type;

    // description
    private String description;
}
