package com.carbon.manage.dto;

import lombok.Data;

/**
 * 管理员新增系统账号参数（整改负责人或另一名管理员）。
 */
@Data
public class CreateUserDTO {

    private String username;
    private String password;
    private String nickname;
    private String role;
    private String phone;
    private String email;
}
