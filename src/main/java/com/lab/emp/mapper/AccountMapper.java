package com.lab.emp.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AccountMapper {
    @Select("SELECT password_hash FROM user_account WHERE username = #{username}")
    String findPasswordHash(@Param("username") String username);
}
