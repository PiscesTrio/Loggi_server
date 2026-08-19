package com.example.api.model.vo;

import com.example.api.model.entity.Employee;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * An employee, without their identification number - see {@link DriverVo} for the reasoning.
 */
public record EmployeeVo(
        String id,
        String name,
        String gender,
        String phone,
        String address,
        String department,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updateAt) {

    public static EmployeeVo from(Employee e) {
        return new EmployeeVo(e.getId(), e.getName(), e.getGender(), e.getPhone(),
                e.getAddress(), e.getDepartment(), e.getCreateAt(), e.getUpdateAt());
    }
}
