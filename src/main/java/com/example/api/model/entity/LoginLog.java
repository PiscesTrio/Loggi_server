package com.example.api.model.entity;

import com.example.api.model.enums.Browser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLog {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    // login email
    private String email;

    // login status
    private Integer status;

    // user IP address
    private String ip;

    // login time
    private LocalDateTime date;

    // browser
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Browser browser;
}
