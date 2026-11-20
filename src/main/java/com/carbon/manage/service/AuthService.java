package com.carbon.manage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.JwtUtil;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.dto.LoginDTO;
import com.carbon.manage.dto.LoginVO;
import com.carbon.manage.entity.User;
import com.carbon.manage.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已停用");
        }
        String token = jwtUtil.generate(user);
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }

    public User currentUser() {
        return userMapper.selectById(UserContext.getRequired().id());
    }
}
