package com.example.api.model.vo;

import com.example.api.model.entity.Employee;
import com.example.api.model.enums.Gender;
import java.time.LocalDateTime;

/** An employee, without their identification number - see {@link DriverVo} for the reasoning. */
public record EmployeeVo(
        String id,
        String name,
        Gender gender,
        String phone,
        String address,
        String department,
        LocalDateTime createAt,
        LocalDateTime updateAt) {

    public static EmployeeVo from(Employee e) {
        return new EmployeeVo(
                e.getId(),
                e.getName(),
                e.getGender(),
                e.getPhone(),
                e.getAddress(),
                e.getDepartment(),
                e.getCreateAt(),
                e.getUpdateAt());
    }
}
