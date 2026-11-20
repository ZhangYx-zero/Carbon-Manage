package com.carbon.manage.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carbon.manage.common.Result;
import com.carbon.manage.dto.ChangePasswordDTO;
import com.carbon.manage.dto.CreateUserDTO;
import com.carbon.manage.dto.UpdateProfileDTO;
import com.carbon.manage.entity.User;
import com.carbon.manage.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 管理员：账号列表。 */
    @GetMapping
    public Result<List<User>> list() {
        return Result.ok(userService.listAll());
    }

    /** 管理员：派单下拉的整改负责人列表。 */
    @GetMapping("/operators")
    public Result<List<User>> operators() {
        return Result.ok(userService.listEnabledOperators());
    }

    /** 管理员：新增整改负责人或管理员账号。 */
    @PostMapping
    public Result<User> create(@RequestBody CreateUserDTO dto) {
        return Result.ok(userService.create(dto));
    }

    /** 所有登录用户：修改本人昵称、手机号、邮箱。 */
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return Result.ok(userService.updateProfile(dto));
    }

    /** 所有登录用户：校验原密码后修改本人密码。 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }
}
