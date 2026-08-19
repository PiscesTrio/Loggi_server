package com.example.api.model.enums;

/**
 * The kind of operation an audited method performs, as written into the operation log.
 *
 * <p>Was spelled {@code BusincessType}, with a field {@code busincessType} on
 * {@link com.example.api.model.entity.SystemLog} to match. The misspelling reached the
 * annotation on every audited endpoint and the JSON the log screen reads.
 */
public enum BusinessType {

    OTHER("其他"),
    QUERY("查询"),
    INSERT("新增"),
    UPDATE("更新"),
    DELETE("删除"),
    EXPORT("导出"),
    FORCE("退出");

    /**
     * Final, and no setter. It had one — a public setter on an enum constant, so any caller
     * could rename a shared global for the rest of the JVM's life. Nothing called it.
     */
    private final String name;

    BusinessType(String name) {
        this.name = name;
    }

    /**
     * The Chinese label, for anything that renders one.
     *
     * <p>There used to be a {@code toString()} returning this as well, and it was a trap.
     * Jackson 3 serialises enums through {@code toString()} where Jackson 2 used
     * {@code name()}, so the audit log stored INSERT and the API answered 新增 - the value
     * and its own description disagreeing across one hop. Anything that interpolated the
     * constant into a string had the same problem quietly.
     *
     * <p>Without the override the enum is its name everywhere, and a caller that wants the
     * label asks for it.
     */
    public String getName() {
        return name;
    }
}
