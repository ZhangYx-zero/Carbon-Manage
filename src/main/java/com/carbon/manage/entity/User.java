package com.carbon.manage.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统用户。
 */
@Data
public class User {

    private Long id;
    private String username;

    @JsonIgnore
    private String password;

    private String nickname;
    private String phone;
    private String email;
    private String role;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
