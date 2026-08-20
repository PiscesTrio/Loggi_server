package com.example.api.controller;

import com.example.api.annotation.Log;
import com.example.api.model.dto.CommodityRequest;
import com.example.api.model.entity.Commodity;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.enums.LogModule;
import com.example.api.model.vo.CommodityVo;
import com.example.api.service.CommodityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Commodities", description = "The catalogue of goods this system moves.")
@RestController
@RequestMapping("/api/commodity")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_COMMODITY')")
public class CommodityController {

    @Resource private CommodityService commodityService;

    @Log(module = LogModule.COMMODITY, type = BusinessType.INSERT)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CommodityVo save(@Valid @RequestBody CommodityRequest request) {
        return CommodityVo.from(commodityService.save(toEntity(request)));
    }

    @Log(module = LogModule.COMMODITY, type = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        commodityService.delete(id);
    }

    @Log(module = LogModule.COMMODITY, type = BusinessType.UPDATE)
    // The id identifies the row, so it belongs in the path. It used to arrive inside the
    // body as part of the entity, which meant a caller chose which row an update applied to
    // by editing a field - and a body without one updated nothing while answering 200.
    @PutMapping("/{id}")
    public void update(@PathVariable String id, @Valid @RequestBody CommodityRequest request) {
        Commodity entity = toEntity(request);
        entity.setId(id);
        commodityService.update(entity);
    }

    @Log(module = LogModule.COMMODITY, type = BusinessType.QUERY)
    @GetMapping("")
    public List<CommodityVo> findAll() {
        return commodityService.findAll().stream().map(CommodityVo::from).toList();
    }

    @Log(module = LogModule.COMMODITY, type = BusinessType.QUERY)
    @GetMapping("/search/{name}")
    public List<CommodityVo> findByLikeName(@PathVariable String name) {
        return commodityService.findAllByLikeName(name).stream().map(CommodityVo::from).toList();
    }

    // @Log(module = LogModule.COMMODITY,type = BusinessType.QUERY)
    @GetMapping("/{id}")
    public CommodityVo findById(@PathVariable String id) {
        return CommodityVo.from(commodityService.findById(id));
    }

    /** The request as the entity the service persists. No id: the database assigns it. */
    private static Commodity toEntity(CommodityRequest request) {
        Commodity e = new Commodity();
        e.setName(request.getName());
        e.setPrice(request.getPrice());
        e.setDescription(request.getDescription());
        e.setCount(request.getCount());
        return e;
    }
}
