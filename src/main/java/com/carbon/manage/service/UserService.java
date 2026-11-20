package com.carbon.manage.service;

import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.dto.ChangePasswordDTO;
import com.carbon.manage.dto.CreateUserDTO;
import com.carbon.manage.dto.UpdateProfileDTO;
import com.carbon.manage.entity.User;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.UserMapper;

/**
 * 用户与个人中心服务：管理员维护账号，所有登录用户维护本人资料与密码。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 派单下拉框数据：仅管理员可查看启用的整改负责人账号。
     */
    public List<User> listEnabledOperators() {
        UserContext.requireRole(UserRole.ADMIN);
        return userMapper.selectEnabledOperators();
    }

    /**
     * 账号列表：仅管理员可见，用于个人中心的账号管理。
     */
    public List<User> listAll() {
        UserContext.requireRole(UserRole.ADMIN);
        return userMapper.selectAll();
    }

    /**
     * 管理员新增账号：可创建整改负责人，也可再注册一名管理员。
     */
    public User create(CreateUserDTO dto) {
        UserContext.requireRole(UserRole.ADMIN);
        String username = required(dto.getUsername(), "登录账号");
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(400, "登录账号需为 3~20 位字母、数字或下划线");
        }
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(400, "登录账号已存在");
        }
        String password = required(dto.getPassword(), "初始密码");
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(400, "初始密码长度需为 6~32 位");
        }
        UserRole role = parseRole(dto.getRole());
        String nickname = optional(dto.getNickname());
        if (nickname == null) {
            nickname = username;
        }
        checkNickname(nickname);
        String phone = optional(dto.getPhone());
        String email = optional(dto.getEmail());
        checkContact(phone, email);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(role.name());
        user.setStatus(1);
        userMapper.insert(user);
        return userMapper.selectById(user.getId());
    }

    /**
     * 修改本人资料：只允许改昵称、手机号、邮箱，账号和角色不可自行变更。
     */
    public User updateProfile(UpdateProfileDTO dto) {
        UserContext.LoginUser loginUser = UserContext.getRequired();
        String nickname = required(dto.getNickname(), "昵称");
        checkNickname(nickname);
        String phone = optional(dto.getPhone());
        String email = optional(dto.getEmail());
        checkContact(phone, email);

        int updated = userMapper.updateProfile(loginUser.id(), nickname, phone, email);
        if (updated == 0) {
            throw new BusinessException(404, "账号不存在");
        }
        return userMapper.selectById(loginUser.id());
    }

    /**
     * 修改本人密码：先校验原密码，再用 BCrypt 重新加密写库。
     */
    public void changePassword(ChangePasswordDTO dto) {
        UserContext.LoginUser loginUser = UserContext.getRequired();
        String oldPassword = required(dto.getOldPassword(), "原密码");
        String newPassword = required(dto.getNewPassword(), "新密码");
        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException(400, "新密码长度需为 6~32 位");
        }
        User user = userMapper.selectByUsername(loginUser.username());
        if (user == null) {
            throw new BusinessException(401, "登录状态失效，请重新登录");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        userMapper.updatePassword(loginUser.id(), passwordEncoder.encode(newPassword));
    }

    private UserRole parseRole(String raw) {
        if (StringUtils.hasText(raw)) {
            for (UserRole role : UserRole.values()) {
                if (role.is(raw)) {
                    return role;
                }
            }
        }
        throw new BusinessException(400, "请选择有效的账号角色");
    }

    private void checkNickname(String nickname) {
        if (nickname.length() > 50) {
            throw new BusinessException(400, "昵称长度不能超过 50 个字符");
        }
    }

    private void checkContact(String phone, String email) {
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(400, "手机号格式不正确");
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
    }

    private String required(String value, String label) {
        String cleaned = optional(value);
        if (cleaned == null) {
            throw new BusinessException(400, label + "不能为空");
        }
        return cleaned;
    }

    private String optional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
