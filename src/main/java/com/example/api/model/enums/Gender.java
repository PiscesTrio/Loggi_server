package com.example.api.model.enums;

/**
 * A person's gender as this system records it, for drivers and employees.
 *
 * <p>The column held 男性 / 女性 — Chinese display text, which the client compared against ({@code
 * gender == "男性"}) to decide which avatar to draw. Two things were wrong with that. The value was a
 * UI language, so translating the interface would have meant either translating the stored data or
 * leaving every historical row speaking the old one; and nothing constrained it, so a typo or a
 * client sending 男 stored as readily as the real value and silently took the female branch forever
 * after.
 *
 * <p>Two constants because two is what the application has ever recorded and what its one input
 * offers. That is a statement about this dataset, not about people.
 */
public enum Gender {
    MALE,
    FEMALE
}
