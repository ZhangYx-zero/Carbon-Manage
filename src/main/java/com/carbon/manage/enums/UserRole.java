package com.carbon.manage.enums;

import lombok.Getter;

/**
 * 系统角色：管理员负责基础数据维护与工单派发/审核，整改负责人处理本人被指派的工单。
 */
@Getter
public enum UserRole {

    ADMIN("管理员"),
    OPERATOR("整改负责人");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public boolean is(String raw) {
        return raw != null && name().equalsIgnoreCase(raw);
    }
}
