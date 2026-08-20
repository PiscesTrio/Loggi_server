package com.example.api.controller;

import com.example.api.model.enums.Role;
import com.example.api.model.vo.RoleVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roles", description = "The roles an administrator can be granted.")
@RestController
@RequestMapping("/api/role")
public class RoleController {

    /**
     * The roles that may be granted. {@link Role#ROLES} deliberately omits ROLE_SUPER_ADMIN — there
     * is no public path to granting it.
     */
    @GetMapping("")
    public List<RoleVo> list() {
        return Arrays.stream(Role.ROLES).map(RoleVo::of).toList();
    }
}
