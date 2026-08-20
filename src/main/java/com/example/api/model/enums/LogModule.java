package com.example.api.model.enums;

/**
 * Which part of the system an audited operation belongs to, as written into {@code
 * system_log.module}.
 *
 * <p>Was a free {@code String} on {@link com.example.api.annotation.Log}, filled in with Chinese
 * display text at every one of the twenty-five call sites. Two consequences. The audit table stored
 * a UI language, so an interface in Japanese or English would have left every historical row
 * speaking the old one — the same problem V7 fixed for {@code business_type} and left here. And the
 * value was whatever someone typed: 商品管理 and 商品 管理 are different modules as far as any query is
 * concerned, and nothing would have said so.
 *
 * <p>The constants name the domain, not the screen. {@code DISTRIBUTION_TRACK} was 运输状态 —
 * "transport status" — while every other module was named 管理 ("management"); the odd one out was
 * the tracking endpoint, and calling it that makes it obvious it belongs beside DISTRIBUTION.
 */
public enum LogModule {
    COMMODITY,
    WAREHOUSE,
    EMPLOYEE,
    DRIVER,
    VEHICLE,
    DISTRIBUTION,
    DISTRIBUTION_TRACK
}
