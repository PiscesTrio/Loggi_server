package com.example.api.controller;

import com.example.api.model.entity.Sale;
import com.example.api.service.SaleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "Sales (API only)", description = "No client calls these. The screens were never built, and after S10 that is a decision rather than an omission: the endpoints are documented here and left in place, because the domain is real and the API is the deliverable. See the backend README.")
@RestController
@RequestMapping("/api/sale")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN' ,'ROLE_SALE')")
public class SaleController {
    @Resource
    private SaleService saleService;

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Sale save(@RequestBody Sale sale) {
        return saleService.save(sale);
    }

    @GetMapping("")
    public List<Sale> findAll() {
        return saleService.findAll();
    }

    @GetMapping("/search/{name}")
    public List<Sale> search(@PathVariable String name) {
        return saleService.searchByCompany(name);
    }

}
