package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Vehicle
 */
@Data
@Entity
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //license plate number
    private String number;

    //vehicle type
    private String type;

    //whether it is currently in transit
    private boolean driving;

    private String createAt;

}
