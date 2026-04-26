package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * 仓库
 */
@Data
@Entity
@NoArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //仓库名称
    private String name;

    //仓库负责人
    private String principle;

    private String location;

    //緯度
    private double lat;
    //經度
    private  double lng;

    private String createAt;

}
