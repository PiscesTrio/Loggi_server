package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.EmployeeRequest;
import com.example.api.model.entity.Employee;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.vo.EmployeeVo;
import com.example.api.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Employees (API only)",
        description =
                "No client calls these, for the same reason as Sales. Documented and kept deliberately; see the backend README.")
@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_EMPLOYEE')")
public class EmployeeController {

    @Resource private EmployeeService employeeService;

    @Log(module = "员工管理", type = BusinessType.QUERY)
    @GetMapping("")
    public List<EmployeeVo> findAll() {
        return employeeService.findAll().stream().map(EmployeeVo::from).toList();
    }

    @Log(module = "员工管理", type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public EmployeeVo findById(@PathVariable String id) {
        return EmployeeVo.from(employeeService.findById(id));
    }

    @Log(module = "员工管理", type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeVo save(@Valid @RequestBody EmployeeRequest request) {
        return EmployeeVo.from(employeeService.save(toEntity(request)));
    }

    @Log(module = "员工管理", type = BusinessType.UPDATE)
    // The id identifies the row, so it belongs in the path. It used to arrive inside the
    // body as part of the entity, which meant a caller chose which row an update applied to
    // by editing a field - and a body without one updated nothing while answering 200.
    @PutMapping("/{id}")
    public void update(@PathVariable String id, @Valid @RequestBody EmployeeRequest request) {
        Employee entity = toEntity(request);
        entity.setId(id);
        employeeService.update(entity);
    }

    @Log(module = "员工管理", type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        employeeService.delete(id);
    }

    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Employee toEntity(EmployeeRequest request) {
        Employee e = new Employee();
        e.setName(request.getName());
        e.setGender(request.getGender());
        e.setPhone(request.getPhone());
        e.setAddress(request.getAddress());
        e.setIdCard(request.getIdCard());
        e.setDepartment(request.getDepartment());
        return e;
    }
}
