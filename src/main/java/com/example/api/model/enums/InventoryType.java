package com.example.api.model.enums;

/**
 * Which way stock moved.
 *
 * <p>Was an Integer holding +1 or -1, with the two constants declared privately inside {@code
 * InventoryRecordServiceImpl} — so the meaning of the column lived in one service's private fields,
 * and every other reader of the column had to know it by hearsay. The client encodes the same two
 * values in a boolean it calls {@code inOrOut}.
 *
 * <p>The sign was doing double duty: it labelled the movement and it looked like something you
 * could multiply by. Nothing ever did, but a number that means "out" is one refactor away from
 * being added to a quantity.
 */
public enum InventoryType {
    IN,
    OUT
}
