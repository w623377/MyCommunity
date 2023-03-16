package com.example.mycommunity.dao;


import com.example.mycommunity.entity.User;
import org.apache.ibatis.annotations.Mapper;
//用户
@Mapper
public interface UserMapper {

    User selectById(int id);//根据id查询用户

    User selectByName(String username);//根据用户名查询用户

    User selectByEmail(String email);//根据邮箱查询用户

    int insertUser(User user);//插入用户

    int updateStatus(int id, int status);//更新用户状态

    int updateHeader(int id, String headerUrl);//更新用户头像

    int updatePassword(int id, String password);//更新用户密码

}
