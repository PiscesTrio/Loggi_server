package com.example.api.controller;

import com.example.api.model.dto.SaleRequest;
import com.example.api.model.entity.Sale;
import com.example.api.model.vo.SaleVo;
import com.example.api.service.SaleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
    public SaleVo save(@Valid @RequestBody SaleRequest request) {
        return SaleVo.from(saleService.save(toEntity(request)));
    }

    @GetMapping("")
    public List<SaleVo> findAll() {
        return saleService.findAll().stream().map(SaleVo::from).toList();
    }

    @GetMapping("/search/{name}")
    public List<SaleVo> search(@PathVariable String name) {
        return saleService.searchByCompany(name).stream().map(SaleVo::from).toList();
    }


    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Sale toEntity(SaleRequest request) {
        Sale e = new Sale();
        e.setCompany(request.getCompany());
        e.setNumber(request.getNumber());
        e.setCommodity(request.getCommodity());
        e.setCount(request.getCount());
        e.setPrice(request.getPrice());
        e.setPhone(request.getPhone());
        e.setDescription(request.getDescription());
        e.setPay(request.isPay());
        return e;
    }
}
