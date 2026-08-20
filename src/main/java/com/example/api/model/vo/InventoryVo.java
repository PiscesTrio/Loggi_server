package com.example.api.model.vo;

import com.example.api.model.entity.Commodity;
import com.example.api.model.entity.Inventory;
import com.example.api.model.entity.Warehouse;

/**
 * How much of one commodity a warehouse holds.
 *
 * <p>Ids for the two references, as this endpoint has always sent - the columns were {@code wid}
 * and {@code cid}. S09 made them associations and kept the id shape with
 * {@code @JsonIdentityReference}, which writes an id but cannot read a lone one back; a view type
 * is not an entity, so the field is simply a String in both directions.
 *
 * <p>{@code name} stays. It is a denormalised copy of the commodity's name and it is what the stock
 * screen renders, so removing it here would mean the client fetching every commodity to label a
 * list it already has.
 */
public record InventoryVo(
        String id,
        String warehouseId,
        String commodityId,
        String name,
        String location,
        Integer count) {

    public static InventoryVo from(Inventory inventory) {
        Warehouse warehouse = inventory.getWarehouse();
        Commodity commodity = inventory.getCommodity();
        return new InventoryVo(
                inventory.getId(),
                warehouse == null ? null : warehouse.getId(),
                commodity == null ? null : commodity.getId(),
                inventory.getName(),
                inventory.getLocation(),
                inventory.getCount());
    }
}
