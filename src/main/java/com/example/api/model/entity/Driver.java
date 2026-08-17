package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 驾驶员
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

    //家庭住址
    private String address;

    //身份证号码
    private String idCard;

    //驾驶证
    private String license;

    //驾驶证分数 12分
    private String score;

    //正在驾驶
    private boolean driving;

    private String createAt;

    private String updateAt;

}
