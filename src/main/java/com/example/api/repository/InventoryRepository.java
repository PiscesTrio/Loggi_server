package com.example.api.repository;

import com.example.api.model.entity.Inventory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Inventory findByWarehouseIdAndCommodityId(String warehouseId, String commodityId);

    List<Inventory> findAllByCommodityId(String commodityId);

    List<Inventory> findAllByWarehouseId(String warehouseId);

    /**
     * Takes {@code quantity} out of a warehouse's stock, if and only if that much is there.
     *
     * <p>The check and the write are one statement, so the database decides both under the row lock
     * the UPDATE takes. Two concurrent withdrawals queue on that lock and the second re-evaluates
     * {@code count >= :quantity} against what the first left behind - which is what a read in Java
     * followed by a save cannot do. Returns the number of rows changed: 1 if the stock was taken, 0
     * if there was no such row or not enough in it.
     *
     * <p>{@code updateAt} is set by hand because a bulk UPDATE bypasses the entity listener that
     * normally fills it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update Inventory i set i.count = i.count - :quantity, i.updateAt = :now"
                    + " where i.warehouse.id = :warehouseId and i.commodity.id = :commodityId"
                    + " and i.count >= :quantity")
    int withdraw(
            @Param("warehouseId") String warehouseId,
            @Param("commodityId") String commodityId,
            @Param("quantity") int quantity,
            @Param("now") LocalDateTime now);

    /**
     * Adds {@code quantity} to a warehouse's stock as one statement, so concurrent deposits are all
     * counted. Returns 0 when the warehouse has never held the commodity; the caller creates the
     * row.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update Inventory i set i.count = coalesce(i.count, 0) + :quantity, i.updateAt = :now"
                    + " where i.warehouse.id = :warehouseId and i.commodity.id = :commodityId")
    int deposit(
            @Param("warehouseId") String warehouseId,
            @Param("commodityId") String commodityId,
            @Param("quantity") int quantity,
            @Param("now") LocalDateTime now);
}
