package com.carbon.manage.dto;

import lombok.Data;

/**
 * 个人中心修改密码参数：必须校验原密码。
 */
@Data
public class ChangePasswordDTO {

    private String oldPassword;
    private String newPassword;
}
