package com.example.api.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Warehouse
 */
@Data
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class Warehouse extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //warehouse name
    private String name;

    //warehouse manager
    private String principle;

    private String location;

    //latitude
    // Coordinates stay double, without a precision/scale annotation. Adding one was tried:
    // Hibernate refuses to build the SessionFactory with "scale has no meaning for SQL
    // floating point types", so pinning decimal storage would mean changing the Java type to
    // BigDecimal — which buys exactness that nothing here needs (every consumer of these
    // values, from distance maths to the map layer, is floating point anyway) and costs a
    // migration plus a change in how the numbers serialise.
    private double lat;
    //longitude
    private  double lng;

}
