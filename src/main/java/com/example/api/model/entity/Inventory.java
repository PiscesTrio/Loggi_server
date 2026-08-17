package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Inventory
 */
@Data
@Entity
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //warehouse id
    private String wid;

    //commodity id
    private String cid;

    //commodity name
    private String name;

    //storage location / area
    private String location;

    //quantity
    private Integer count;

}
