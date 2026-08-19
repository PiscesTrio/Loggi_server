package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.entity.Employee;
import com.example.api.model.enums.BusinessType;
import com.example.api.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_EMPLOYEE')")
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @Log(module = "员工管理",type = BusinessType.QUERY)
    @GetMapping("")
    public List<Employee> findAll() {
        return employeeService.findAll();
    }

    @Log(module = "员工管理",type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public Employee findById(@PathVariable String id) {
        return employeeService.findById(id);
    }

    @Log(module = "员工管理",type = BusinessType.INSERT)
    @PostMapping("")
    public Employee save(@RequestBody Employee employee) {
        return employeeService.save(employee);
    }

    @Log(module = "员工管理",type = BusinessType.UPDATE)
    @PutMapping("")
    public void update(@RequestBody Employee employee) {
        employeeService.update(employee);
    }

    @Log(module = "员工管理",type = BusinessType.DELETE)
    @DeleteMapping("")
    public void delete(String id) {
        employeeService.delete(id);
    }

}
