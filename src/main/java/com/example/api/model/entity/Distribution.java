package com.example.api.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Distribution
 */
@Data
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class Distribution extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    //driver id
    private String did;

    //vehicle id
    private String vid;

    private String wid;

    //driver
    private String driver;

    //license plate number
    private String number;

    //customer phone
    private String phone;

    //customer address
    private String address;

    //expedited handling
    private boolean urgent;

    private String care;
    //operation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    private Integer status;

    private double fromLat;

    private double fromLng;

    private double toLat;

    private double toLng;

}
