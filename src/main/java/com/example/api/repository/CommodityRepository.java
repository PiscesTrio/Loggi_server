package com.example.api.repository;

import com.example.api.model.entity.Commodity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommodityRepository extends JpaRepository<Commodity, String> {

    Commodity findByName(String name);

    List<Commodity> findByNameLike(String name);
}
