package com.example.api.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Inventory record: stock-out / stock-in movement
 */
@Data
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

    //commodity name
    private String name;

    //warehouse id
    private String wid;

    //commodity id
    private String cid;

    private Integer count;

    //-1: stock-out, +1: stock-in
    private Integer type;

    //description
    private String description;

}
