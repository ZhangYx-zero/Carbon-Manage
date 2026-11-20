package com.carbon.manage.common;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.enums.UserRole;

/**
 * 当前登录用户上下文（ThreadLocal），由 JwtInterceptor 写入、请求结束后清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public record LoginUser(Long id, String username, String role) {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser getRequired() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user;
    }

    /**
     * 要求当前用户具备指定角色，否则抛出 403 业务异常。
     */
    public static LoginUser requireRole(UserRole role) {
        LoginUser user = getRequired();
        if (!role.is(user.role())) {
            throw new BusinessException(403, "无权限执行该操作");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
