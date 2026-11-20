package com.carbon.manage.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.carbon.manage.common.BusinessException;

/**
 * 登录拦截器：校验 Authorization: Bearer {token}，通过后将用户写入 UserContext。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String Bearer_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(Bearer_PREFIX)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        try {
            UserContext.set(jwtUtil.parse(header.substring(Bearer_PREFIX.length())));
        } catch (Exception e) {
            throw new BusinessException(401, "登录状态失效，请重新登录");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
