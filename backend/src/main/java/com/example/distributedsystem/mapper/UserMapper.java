package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password, role, nick_name, introduce_sign, address, avatar_url, email, phone, locked_flag, is_deleted, created_at FROM user_account WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO user_account(username, password, role, nick_name, address, avatar_url, email, phone, locked_flag) VALUES(#{username}, #{password}, #{role}, #{nickName}, #{address}, #{avatarUrl}, #{email}, #{phone}, #{lockedFlag})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user_account SET nick_name = #{nickName}, introduce_sign = #{introduceSign}, address = #{address}, avatar_url = #{avatarUrl}, email = #{email}, phone = #{phone} WHERE username = #{username}")
    int update(User user);

    @Update("UPDATE user_account SET password = #{password} WHERE username = #{username}")
    int updatePassword(User user);

    @Select("SELECT id, username, role, nick_name, introduce_sign, address, avatar_url, email, phone, locked_flag, is_deleted, created_at FROM user_account ORDER BY id DESC")
    List<User> findAll();
}
