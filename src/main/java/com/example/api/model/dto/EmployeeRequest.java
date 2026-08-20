package com.example.api.model.dto;

import com.example.api.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** What a caller sends to create or update an employee. */
@Data
public class EmployeeRequest {

    @NotBlank(message = "name is required")
    private String name;

    private Gender gender;

    @NotBlank(message = "phone is required")
    private String phone;

    private String address;

    private String idCard;

    private String department;
}
