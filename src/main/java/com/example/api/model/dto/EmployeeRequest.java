package com.example.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** What a caller sends to create or update an employee. */
@Data
public class EmployeeRequest {

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String gender;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    private String address;

    private String idCard;

    private String department;
}
