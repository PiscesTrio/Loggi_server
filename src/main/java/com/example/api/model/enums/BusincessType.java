package com.example.api.model.enums;
/*
    Business operation type
 */
public enum BusincessType {
    OTHER("其他"), //other
    QUERY("查询"), //query
    INSERT("新增"), //insert
    UPDATE("更新"), //update
    DELETE("删除"), //delete
    EXPORT("导出"), //export
    FORCE("退出"); //force logout

    private BusincessType(String name){
        this.name=name;
    }
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
