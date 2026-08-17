package com.example.api.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * 验证码
 */
@Data
@Entity
@NoArgsConstructor
public class Code {

    @Id
    private String email;

    private String value;

    private long exp;

    public Code(String email, String value) {
        this.email = email;
        this.value = value;
        this.exp = System.currentTimeMillis() + 1000 * 60 * 15;
    }

}
