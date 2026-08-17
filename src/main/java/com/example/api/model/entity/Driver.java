package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Driver
 */
@Data
@Entity
@NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String name;

    private String gender;

    private String phone;

    //Home address
    private String address;

    //ID card number
    private String idCard;

    //Driver's license
    private String license;

    //License points, out of 12
    private String score;

    //Currently driving
    private boolean driving;

    private String createAt;

    private String updateAt;

}
