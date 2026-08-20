package com.example.api.repository;

import com.example.api.model.entity.Distribution;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributionRepository extends JpaRepository<Distribution, String> {

    /**
     * Every order, with its driver, vehicle and warehouse already loaded.
     *
     * <p>Overridden purely to say that. The associations are LAZY, {@code open-in-view} is false,
     * and this controller returns the entity straight to Jackson — so without an entity graph the
     * proxies are touched after the transaction has closed and every request fails with
     * LazyInitializationException. Loading them one at a time instead would work and would issue
     * three queries per order, which is the N+1 this slice exists to remove.
     *
     * <p>{@code care} joined the list when it stopped being a column and became rows. It is the
     * only collection here, so the join multiplies rows by the number of tags on an order and
     * Hibernate folds them back; a second collection would need a different strategy.
     */
    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle", "warehouse", "care"})
    List<Distribution> findAll();
}
