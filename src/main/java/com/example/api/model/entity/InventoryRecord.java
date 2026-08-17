package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Inventory record: stock-out / stock-in movement
 */
@Data
@Entity
@NoArgsConstructor
public class InventoryRecord {

    @Id
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

    private String createAt;

}
