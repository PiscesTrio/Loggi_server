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

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
