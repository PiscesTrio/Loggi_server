package com.example.api.service.impl;

import com.example.api.model.entity.Sale;
import com.example.api.repository.SaleRepository;
import com.example.api.service.SaleService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SaleServiceImpl implements SaleService {

    @Resource private SaleRepository saleRepository;

    @Override
    public Sale save(Sale sale) {
        return saleRepository.save(sale);
    }

    @Override
    public List<Sale> findAll() {
        return saleRepository.findAll();
    }

    @Override
    public List<Sale> searchByCompany(String name) {
        return saleRepository.findAllByCompanyLike(name);
    }
}
