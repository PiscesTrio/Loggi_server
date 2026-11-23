package com.example.api.repository;

import com.example.api.model.entity.Distribution;
import com.example.api.model.enums.DistributionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributionRepository extends JpaRepository<Distribution, String> {

    /**
     * Every order, with its driver, vehicle and warehouse already loaded.
     *
     * <p>Overridden purely to say that. The associations are LAZY, {@code open-in-view} is false,
     * and the controller maps the entity to a DistributionVo outside the transaction — so without
     * an entity graph the proxies are touched after it has closed and every request fails with
     * LazyInitializationException. Loading them one at a time instead would work and would issue
     * three queries per order, which is the N+1 this entity graph exists to remove.
     *
     * <p>{@code care} joined the list when it stopped being a column and became rows. It is the
     * only collection here, so the join multiplies rows by the number of tags on an order and
     * Hibernate folds them back; a second collection would need a different strategy.
     */
    @Override
    @EntityGraph(attributePaths = {"driver", "vehicle", "warehouse", "care"})
    List<Distribution> findAll();

    /** One order, loaded the same way as {@link #findAll} so it can be rendered after commit. */
    @EntityGraph(attributePaths = {"driver", "vehicle", "warehouse", "care"})
    Optional<Distribution> findWithAssociationsById(String id);

    /**
     * Moves an order from {@code from} to {@code to}, and only if it is still in {@code from}.
     *
     * <p>The expected status is part of the WHERE clause so that two people approving the same
     * order at once cannot both succeed: the second UPDATE finds the order already moved, changes
     * no row, and returns 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update Distribution d set d.status = :to, d.updateAt = :now"
                    + " where d.id = :id and d.status = :from")
    int moveStatus(
            @Param("id") String id,
            @Param("from") DistributionStatus from,
            @Param("to") DistributionStatus to,
            @Param("now") LocalDateTime now);
}
