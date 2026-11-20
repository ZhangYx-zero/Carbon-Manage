package com.carbon.manage.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

import com.carbon.manage.common.BusinessException;

/**
 * 工单状态机：待派发 -> 整改中 -> 待审核 -> 已完成，审核不通过时待审核退回整改中。
 * <p>允许转移关系集中定义在枚举内部，Service 层据此做防错校验，杜绝非法状态篡改。</p>
 * <p>待派发与待审核都能转移到整改中，两条边的语义（派发/驳回）由 Service 的起始状态校验区分。</p>
 */
@Getter
public enum OrderStatus {

    PENDING_DISPATCH("待派发"),
    RECTIFYING("整改中"),
    PENDING_AUDIT("待审核"),
    COMPLETED("已完成");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_DISPATCH, EnumSet.of(RECTIFYING),
            RECTIFYING, EnumSet.of(PENDING_AUDIT),
            PENDING_AUDIT, EnumSet.of(COMPLETED, RECTIFYING),
            COMPLETED, EnumSet.noneOf(OrderStatus.class));

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public boolean canGoTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public static OrderStatus of(String value) {
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BusinessException(400, "未知的工单状态：" + value);
    }
}
