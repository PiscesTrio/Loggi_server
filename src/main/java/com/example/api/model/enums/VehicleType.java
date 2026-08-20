package com.example.api.model.enums;

/**
 * The class of vehicle a delivery is assigned to.
 *
 * <p>Stored as 货车 / 卡车 / 重卡 — the exact strings the client's dropdown offered, which the client
 * then switched on to pick an icon. The dropdown and the database therefore had to agree character
 * for character, and nothing checked that they did: the seed file carries a comment warning that
 * these values "must remain exactly as they are, because the client compares against them", which
 * is a comment doing a type's job.
 */
public enum VehicleType {
    LIGHT_TRUCK,
    TRUCK,
    HEAVY_TRUCK
}
