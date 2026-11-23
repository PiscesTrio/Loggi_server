package com.example.api.repository;

import com.example.api.model.entity.Commodity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommodityRepository extends JpaRepository<Commodity, String> {

    Commodity findByName(String name);

    List<Commodity> findByNameLike(String name);

    /**
     * Moves a commodity's total by {@code delta} in one statement, refusing to take it below zero.
     *
     * <p>Same shape as {@link InventoryRepository#withdraw}: the arithmetic happens in the database
     * under the row lock, so concurrent movements add up instead of overwriting each other. Returns
     * 0 if the commodity does not exist or the total would go negative.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update Commodity c set c.count = c.count + :delta, c.updateAt = :now"
                    + " where c.id = :id and c.count + :delta >= 0")
    int adjustCount(
            @Param("id") String id, @Param("delta") int delta, @Param("now") LocalDateTime now);
}
