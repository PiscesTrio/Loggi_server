package com.example.api.model.entity;

import com.example.api.model.enums.BusinessType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    //primary key
    private String id;
    //account
    private String account;
    //functional module
    private String module;

    //operation type
    @Column(columnDefinition = "varchar(30) default 'LTD' not null")
    private String businessType;

    //user IP
    @Column(columnDefinition = "varchar(40) default 'LTD' not null")
    private String ip;

    //request method
    @Column(columnDefinition = "varchar(100) default 'LTD' not null")
    private String method;
    //operation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    //how long the operation took, in milliseconds
    //LogAspect measured this from the first version and had nowhere to put it
    private Long costMs;

    //whether the operation completed or threw
    //the aspect records from a finally block, so a failed operation is written too - it was
    //previously indistinguishable from one that worked
    @Column(columnDefinition = "boolean default true not null")
    private boolean success;

}
