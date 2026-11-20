package com.carbon.manage.dto;

import lombok.Data;

/**
 * 个人中心资料修改参数：账号与角色不可自行修改。
 */
@Data
public class UpdateProfileDTO {

    private String nickname;
    private String phone;
    private String email;
}
