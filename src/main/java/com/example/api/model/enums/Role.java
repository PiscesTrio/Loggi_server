package com.example.api.model.enums;

import lombok.Getter;

/**
 * The roles an administrator can hold.
 *
 * <p>Each constant used to carry a Chinese description alongside its value, and {@code GET
 * /api/role} handed both to the client. That put display text in the one type the security rules
 * switch on, and it put a language choice on the wire: a client rendering a Japanese or English UI
 * received {@code 商品相关权限} and had nowhere to go with it. The value is the identifier; what a reader
 * should be shown for it is a decision only the reader's locale can make.
 */
@Getter
public enum Role {

    // super admin: no public method is exposed for granting this role
    ROLE_SUPER_ADMIN("ROLE_SUPER_ADMIN"),

    ROLE_ADMIN("ROLE_ADMIN"),

    ROLE_COMMODITY("ROLE_COMMODITY"),

    ROLE_EMPLOYEE("ROLE_EMPLOYEE"),

    ROLE_SALE("ROLE_SALE"),

    ROLE_WAREHOUSE("ROLE_WAREHOUSE");

    private final String value;

    public static final Role[] ROLES = {
        ROLE_ADMIN, ROLE_COMMODITY, ROLE_EMPLOYEE, ROLE_SALE, ROLE_WAREHOUSE
    };

    Role(String value) {
        this.value = value;
    }
}
