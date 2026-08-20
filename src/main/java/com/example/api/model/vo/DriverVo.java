package com.example.api.model.vo;

import com.example.api.model.entity.Driver;
import com.example.api.model.enums.Gender;
import java.time.LocalDateTime;

/**
 * A driver, as the API describes one.
 *
 * <p>Without {@code idCard}. The entity holds a personal identification number and the entity was
 * what every list endpoint returned, so that number went to every authenticated caller - while no
 * screen in the client has ever displayed it. Data that nothing asks for and nobody displays is
 * data that only creates exposure.
 *
 * <p>{@code license} stays: which class a driver holds decides what they may be dispatched with, so
 * it is operational rather than personal.
 */
public record DriverVo(
        String id,
        String name,
        Gender gender,
        String phone,
        String address,
        String license,
        String score,
        boolean driving,
        LocalDateTime createAt,
        LocalDateTime updateAt) {

    public static DriverVo from(Driver d) {
        return new DriverVo(
                d.getId(),
                d.getName(),
                d.getGender(),
                d.getPhone(),
                d.getAddress(),
                d.getLicense(),
                d.getScore(),
                d.isDriving(),
                d.getCreateAt(),
                d.getUpdateAt());
    }
}
