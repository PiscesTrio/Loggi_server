package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Employee
 */
@Data
@Entity
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;
    //name
    private String name;
    //gender
    private String gender;
    //mobile number
    private String phone;
    //home address
    private String address;
    //ID card number
    private String idCard;
    //department
    private String department;
    //created at
    private String createAt;
    //updated at
    private String updateAt;

}
