package com.carbon.manage.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.carbon.manage.entity.User;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    List<User> selectAll();

    List<User> selectEnabledOperators();

    int countByUsername(@Param("username") String username);

    int insert(User user);

    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("phone") String phone,
                      @Param("email") String email);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
